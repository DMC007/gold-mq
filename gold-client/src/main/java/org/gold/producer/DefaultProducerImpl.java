package org.gold.producer;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.dto.*;
import org.gold.enums.*;
import org.gold.event.EventBus;
import org.gold.netty.BrokerRemoteRespHandler;
import org.gold.remote.BrokerNettyRemoteClient;
import org.gold.remote.NameServerNettyRemoteClient;
import org.gold.transaction.TransactionListener;
import org.gold.utils.AssertUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description 提供基础的mq发送功能
 */
public class DefaultProducerImpl implements Producer {

    private static final Logger log = LogManager.getLogger(DefaultProducerImpl.class);

    private String nameserverIp;
    private Integer nameserverPort;
    private String nameserverUser;
    private String nameserverPassword;
    private String brokerClusterGroup;
    private String brokerRole = "single";
    private List<String> brokerAddressList;
    private List<String> masterAddressList;
    //事务相关属性
    private TransactionListener transactionListener;
    private String producerId;
    private NameServerNettyRemoteClient nameServerNettyRemoteClient;
    private Map<String, BrokerNettyRemoteClient> brokerNettyRemoteClientMap = new ConcurrentHashMap<>();

    public void start() {
        String registerMsgId = UUID.randomUUID().toString();
        nameServerNettyRemoteClient = new NameServerNettyRemoteClient(nameserverIp, nameserverPort);
        nameServerNettyRemoteClient.buildConnection();
        //开始注册
        ServiceRegistryReqDTO serviceRegistryReqDTO = new ServiceRegistryReqDTO();
        serviceRegistryReqDTO.setRegistryType(RegistryTypeEnum.PRODUCER.getCode());
        serviceRegistryReqDTO.setUser(nameserverUser);
        serviceRegistryReqDTO.setPassword(nameserverPassword);
        serviceRegistryReqDTO.setMsgId(registerMsgId);
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.REGISTRY.getCode(), JSON.toJSONBytes(serviceRegistryReqDTO));
        TcpMsg tcpMsgRes = nameServerNettyRemoteClient.sendSyncMsg(tcpMsg, registerMsgId);
        if (NameServerResponseCode.REGISTRY_SUCCESS.getCode() != tcpMsgRes.getCode()) {
            log.error("producer register fail");
            return;
        }
        //给生产者分配一个唯一ID值
        this.producerId = UUID.randomUUID().toString();
        //开启心跳检测
        startHeartBeatTask();
        fetchBrokerAddress();
        //定时刷新broker地址
        startRefreshBrokerAddressTask();
    }


    @Override
    public SendResult send(MessageDTO message) {
        BrokerNettyRemoteClient remoteClient = this.getBrokerNettyRemoteClient();
        String msgId = UUID.randomUUID().toString();
        message.setMsgId(msgId);
        message.setSendWay(MessageSendWay.SYNC.getCode());
        TcpMsg tcpMsg = new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(message));
        //注意这里是公共的封装逻辑，返回值底层是通过自定义future获取实现同步效果
        TcpMsg tcpMsgRes = remoteClient.sendSyncMsg(tcpMsg, msgId);
        SendMessageToBrokerResponseDTO sendMessageToBrokerResponseDTO = JSON.parseObject(tcpMsgRes.getBody(), SendMessageToBrokerResponseDTO.class);
        int status = sendMessageToBrokerResponseDTO.getStatus();
        SendResult sendResult = new SendResult();
        if (status == SendMessageToBrokerResponseStatus.SUCCESS.getCode()) {
            sendResult.setSendStatus(SendStatus.SUCCESS);
        } else if (status == SendMessageToBrokerResponseStatus.FAIL.getCode()) {
            sendResult.setSendStatus(SendStatus.FAILURE);
            log.error("send message fail, desc:{}", sendMessageToBrokerResponseDTO.getDesc());
        }
        return sendResult;
    }

    @Override
    public void sendAsync(MessageDTO message) {
        BrokerNettyRemoteClient remoteClient = this.getBrokerNettyRemoteClient();
        message.setSendWay(MessageSendWay.ASYNC.getCode());
        TcpMsg tcpMsg = new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(message));
        remoteClient.sendAsyncMsg(tcpMsg);
    }

    @Override
    public SendResult sendTxMessage(MessageDTO message) {
        AssertUtils.isNotNull(message, "message can not be null");
        AssertUtils.isNotNull(transactionListener, "transactionListener can not be null");
        String msgId = UUID.randomUUID().toString();
        message.setTxFlag(TxMessageFlagEnum.HALF_MSG.getCode());
        message.setMsgId(msgId);
        message.setProducerId(producerId);
        message.setSendWay(MessageSendWay.SYNC.getCode());
        TcpMsg tcpMsg = new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(message));
        BrokerNettyRemoteClient remoteClient = this.getBrokerNettyRemoteClient();
        TcpMsg tcpMsgRes = remoteClient.sendSyncMsg(tcpMsg, msgId);
        boolean isHalfMsgSendSuccess = tcpMsgRes != null && tcpMsgRes.getCode() == BrokerResponseCode.HALF_MSG_SEND_SUCCESS.getCode();
        if (!isHalfMsgSendSuccess) {
            log.error("send half message fail");
            throw new RuntimeException("send half message fail");
        }
        //发送半消息成功后，执行本地事务代码
        LocalTransactionState localTransactionState = transactionListener.executeLocalTransaction(message);
        AssertUtils.isNotNull(localTransactionState, "localTransactionState can not be null");
        //设置本地事务执行后的状态
        message.setLocalTxState(localTransactionState.getCode());
        if (LocalTransactionState.COMMIT.equals(localTransactionState)) {
            message.setTxFlag(TxMessageFlagEnum.REMAIN_HALF_ACK.getCode());
            TcpMsg remainHalfAckMsg = new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(message));
            TcpMsg remainHalfAckMsgRes = remoteClient.sendSyncMsg(remainHalfAckMsg, msgId);
            log.info("remain half ack message res: {}", JSON.toJSONString(remainHalfAckMsgRes));
        } else if (LocalTransactionState.ROLLBACK.equals(localTransactionState)) {
            //通知到broker本地事务消息执行失败，不然broker会一直回调客户端查询状态，会有额外性能损耗
            message.setTxFlag(TxMessageFlagEnum.REMAIN_HALF_ACK.getCode());
            TcpMsg rollbackMsg = new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(message));
            TcpMsg rollbackMsgRes = remoteClient.sendSyncMsg(rollbackMsg, msgId);
            log.info("rollback message res: {}", JSON.toJSONString(rollbackMsgRes));
        } else if (LocalTransactionState.UNKNOW.equals(localTransactionState)) {
            //TODO 等待broker回调查询状态判断
        }
        //本地事务执行环节
        SendResult sendResult = new SendResult();
        sendResult.setSendStatus(SendStatus.SUCCESS);
        return sendResult;
    }

    private void startHeartBeatTask() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                    log.info("send heart beat");
                    HeartBeatDTO heartBeatDTO = new HeartBeatDTO();
                    String heartBeatMsgId = UUID.randomUUID().toString();
                    heartBeatDTO.setMsgId(heartBeatMsgId);
                    //发送心跳数据
                    TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.HEART_BEAT.getCode(), JSON.toJSONBytes(heartBeatDTO));
                    TcpMsg tcpMsgRes = nameServerNettyRemoteClient.sendSyncMsg(tcpMsg, heartBeatMsgId);
                    if (NameServerResponseCode.HEART_BEAT_SUCCESS.getCode() != tcpMsgRes.getCode()) {
                        log.error("heart beat fail");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "heart-beat-task");
        thread.start();
    }

    private void fetchBrokerAddress() {
        String brokerAddressMsgId = UUID.randomUUID().toString();
        PullBrokerIpDTO reqDTO = new PullBrokerIpDTO();
        reqDTO.setMsgId(brokerAddressMsgId);
        if (brokerClusterGroup != null) {
            brokerRole = "master";
            reqDTO.setBrokerClusterGroup(brokerClusterGroup);
        }
        reqDTO.setRole(brokerRole);
        //构建请求消息
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.PULL_BROKER_IP_LIST.getCode(), JSON.toJSONBytes(reqDTO));
        TcpMsg tcpMsgRes = nameServerNettyRemoteClient.sendSyncMsg(tcpMsg, brokerAddressMsgId);
        PullBrokerIpRespDTO brokerIpRespDTO = JSON.parseObject(tcpMsgRes.getBody(), PullBrokerIpRespDTO.class);
        brokerAddressList = brokerIpRespDTO.getAddressList();
        masterAddressList = brokerIpRespDTO.getMasterAddressList();
        log.info("fetch broker address:{}, master:{}", brokerAddressList, masterAddressList);
        //获得broker地址后与其建立长连接
        connectBroker();
    }

    /**
     * 连接broker
     */
    private void connectBroker() {
        List<String> brokerAddressList = new ArrayList<>();
        if ("single".equals(this.brokerRole)) {
            AssertUtils.isNotEmpty(brokerAddressList, "broker address list is empty");
            brokerAddressList = this.brokerAddressList;
        } else if ("master".equals(this.brokerRole)) {
            AssertUtils.isNotEmpty(masterAddressList, "master broker address list is empty");
            brokerAddressList = this.masterAddressList;
        }
        //判断之前是否有连接过目标地址，以及连接是否正常，如果连接正常则没必要重新连接，避免无意义的通讯中断情况发生
        List<BrokerNettyRemoteClient> newBrokerNettyRemoteClientList = new ArrayList<>();
        for (String brokerAddress : brokerAddressList) {
            BrokerNettyRemoteClient brokerNettyRemoteClient = brokerNettyRemoteClientMap.get(brokerAddress);
            if (brokerNettyRemoteClient == null) {
                //说明之前没有连接过，需要额外连接接入
                String[] brokerAddressArr = brokerAddress.split(":");
                BrokerNettyRemoteClient newBrokerNettyRemoteClient = new BrokerNettyRemoteClient(brokerAddressArr[0], Integer.parseInt(brokerAddressArr[1]));
                newBrokerNettyRemoteClient.buildConnection(new BrokerRemoteRespHandler(new EventBus("producer-client-eventbus")));
                newBrokerNettyRemoteClientList.add(newBrokerNettyRemoteClient);
                continue;
            } else if (brokerNettyRemoteClient.isChannelActive()) {
                //连接正常，不需要重新连接
                newBrokerNettyRemoteClientList.add(brokerNettyRemoteClient);
                continue;
            }
            //到这里的就是连接中断的, 尝试重新连接
            String[] brokerAddressArr = brokerAddress.split(":");
            BrokerNettyRemoteClient newBrokerNettyRemoteClient = new BrokerNettyRemoteClient(brokerAddressArr[0], Integer.parseInt(brokerAddressArr[1]));
            newBrokerNettyRemoteClient.buildConnection(new BrokerRemoteRespHandler(new EventBus("producer-client-eventbus")));
            //添加到连接列表中
            newBrokerNettyRemoteClientList.add(newBrokerNettyRemoteClient);
        }
        List<String> findBrokerAddressList = brokerAddressList;
        List<String> needRemoveBrokerIds = brokerNettyRemoteClientMap.keySet()
                .stream().filter(reqId -> !findBrokerAddressList.contains(reqId)).toList();
        for (String brokerId : needRemoveBrokerIds) {
            //关闭无用的连接
            brokerNettyRemoteClientMap.get(brokerId).close();
            brokerNettyRemoteClientMap.remove(brokerId);
        }
        brokerNettyRemoteClientMap = newBrokerNettyRemoteClientList.stream()
                .collect(Collectors.toMap(BrokerNettyRemoteClient::getBrokerReqId, Function.identity()));
    }

    public void startRefreshBrokerAddressTask() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                    log.info("refresh broker address");
                    fetchBrokerAddress();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "refresh-broker-address-task");
        thread.start();
    }

    private BrokerNettyRemoteClient getBrokerNettyRemoteClient() {
        return this.getBrokerNettyRemoteClientMap().values().stream().toList().getFirst();
    }

    public String getNameserverIp() {
        return nameserverIp;
    }

    public void setNameserverIp(String nameserverIp) {
        this.nameserverIp = nameserverIp;
    }

    public Integer getNameserverPort() {
        return nameserverPort;
    }

    public void setNameserverPort(Integer nameserverPort) {
        this.nameserverPort = nameserverPort;
    }

    public String getNameserverUser() {
        return nameserverUser;
    }

    public void setNameserverUser(String nameserverUser) {
        this.nameserverUser = nameserverUser;
    }

    public String getNameserverPassword() {
        return nameserverPassword;
    }

    public void setNameserverPassword(String nameserverPassword) {
        this.nameserverPassword = nameserverPassword;
    }

    public String getBrokerClusterGroup() {
        return brokerClusterGroup;
    }

    public void setBrokerClusterGroup(String brokerClusterGroup) {
        this.brokerClusterGroup = brokerClusterGroup;
    }

    public String getBrokerRole() {
        return brokerRole;
    }

    public void setBrokerRole(String brokerRole) {
        this.brokerRole = brokerRole;
    }

    public List<String> getBrokerAddressList() {
        return brokerAddressList;
    }

    public void setBrokerAddressList(List<String> brokerAddressList) {
        this.brokerAddressList = brokerAddressList;
    }

    public List<String> getMasterAddressList() {
        return masterAddressList;
    }

    public void setMasterAddressList(List<String> masterAddressList) {
        this.masterAddressList = masterAddressList;
    }

    public TransactionListener getTransactionListener() {
        return transactionListener;
    }

    public void setTransactionListener(TransactionListener transactionListener) {
        this.transactionListener = transactionListener;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public NameServerNettyRemoteClient getNameServerNettyRemoteClient() {
        return nameServerNettyRemoteClient;
    }

    public void setNameServerNettyRemoteClient(NameServerNettyRemoteClient nameServerNettyRemoteClient) {
        this.nameServerNettyRemoteClient = nameServerNettyRemoteClient;
    }

    public Map<String, BrokerNettyRemoteClient> getBrokerNettyRemoteClientMap() {
        return brokerNettyRemoteClientMap;
    }

    public void setBrokerNettyRemoteClientMap(Map<String, BrokerNettyRemoteClient> brokerNettyRemoteClientMap) {
        this.brokerNettyRemoteClientMap = brokerNettyRemoteClientMap;
    }
}
