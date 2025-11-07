package org.gold.producer;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.dto.*;
import org.gold.enums.NameServerEventCode;
import org.gold.enums.NameServerResponseCode;
import org.gold.enums.RegistryTypeEnum;
import org.gold.remote.BrokerNettyRemoteClient;
import org.gold.remote.NameServerNettyRemoteClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private List<String> brodkerAddressList;
    private List<String> masterAddressList;
    //TODO 事务相关属性
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
    }


    @Override
    public SendResult send(MessageDTO message) {
        return null;
    }

    @Override
    public void sendAsync(MessageDTO message) {

    }

    @Override
    public SendResult sendTxMessage(MessageDTO message) {
        return null;
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
        brodkerAddressList = brokerIpRespDTO.getAddressList();
        masterAddressList = brokerIpRespDTO.getMasterAddressList();
        log.info("fetch broker address:{}, master:{}", brodkerAddressList, masterAddressList);
        connectBroker();
    }

    /**
     * 连接broker
     */
    private void connectBroker() {

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

    public List<String> getBrodkerAddressList() {
        return brodkerAddressList;
    }

    public void setBrodkerAddressList(List<String> brodkerAddressList) {
        this.brodkerAddressList = brodkerAddressList;
    }

    public List<String> getMasterAddressList() {
        return masterAddressList;
    }

    public void setMasterAddressList(List<String> masterAddressList) {
        this.masterAddressList = masterAddressList;
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
