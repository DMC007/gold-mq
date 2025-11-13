package org.gold.consumer;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.dto.HeartBeatDTO;
import org.gold.dto.PullBrokerIpDTO;
import org.gold.dto.PullBrokerIpRespDTO;
import org.gold.dto.ServiceRegistryReqDTO;
import org.gold.enums.NameServerEventCode;
import org.gold.enums.NameServerResponseCode;
import org.gold.enums.RegistryTypeEnum;
import org.gold.event.EventBus;
import org.gold.netty.BrokerRemoteRespHandler;
import org.gold.producer.DefaultProducerImpl;
import org.gold.remote.BrokerNettyRemoteClient;
import org.gold.remote.NameServerNettyRemoteClient;
import org.gold.utils.AssertUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhaoxun
 * @date 2025/11/12
 */
public class DefaultMqConsumer {
    private static final Logger log = LogManager.getLogger(DefaultProducerImpl.class);

    //如果broker有数据，每间隔100ms拉取一次
    private final static int EACH_BATCH_PULL_MSG_INTER = 100;
    //如果broker没有数据，则每间隔1s拉取一次
    private final static int EACH_BATCH_PULL_MSG_INTER_WHEN_NO_MSG = 1000;


    private String nameserverIp;
    private Integer nameserverPort;
    private String nameserverUser;
    private String nameserverPassword;
    private String topic;
    private String consumerGroup;
    private String brokerRole = "single";
    private Integer queueId;
    private Integer batchSize;
    private String brokerClusterGroup;
    private NameServerNettyRemoteClient nameServerNettyRemoteClient;
    private List<String> brokerAddressList;
    private List<String> masterAddressList;
    private List<String> slaveAddressList;
    private MessageConsumerListener messageConsumerListener;
    private Map<String, BrokerNettyRemoteClient> brokerNettyRemoteClientMap = new ConcurrentHashMap<>();
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public void start() {
        //连接到nameserver
        nameServerNettyRemoteClient = new NameServerNettyRemoteClient(nameserverIp, nameserverPort);
        nameServerNettyRemoteClient.buildConnection();
        boolean isRegister = this.doRegister();
        if (isRegister) {
            this.startHeartBeatTask();
        }
    }

    /**
     * 注册到nameserver[跟rocketmq的生产消费者初始化不同，这里只是简单的跟nameserver做长连接，实际上可以做成短链接]
     *
     * @return true 注册成功
     */
    private boolean doRegister() {
        String msgId = UUID.randomUUID().toString();
        ServiceRegistryReqDTO serviceRegistryReqDTO = new ServiceRegistryReqDTO();
        serviceRegistryReqDTO.setRegistryType(RegistryTypeEnum.CONSUMER.getCode());
        serviceRegistryReqDTO.setMsgId(msgId);
        serviceRegistryReqDTO.setUser(nameserverUser);
        serviceRegistryReqDTO.setPassword(nameserverPassword);
        //不需要传递IP跟端口，nameserver会根据channel的方法获取连接的IP跟端口
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.REGISTRY.getCode(), JSON.toJSONBytes(serviceRegistryReqDTO));
        TcpMsg tcpMsgRes = nameServerNettyRemoteClient.sendSyncMsg(tcpMsg, msgId);
        if (NameServerResponseCode.REGISTRY_SUCCESS.getCode() == tcpMsgRes.getCode()) {
            log.info("topic consumer:{} ,register success", topic);
            return true;
        } else {
            log.error("topic consumer:{} ,register fail", topic);
            return false;
        }
    }

    private void startHeartBeatTask() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                    log.info("topic consumer:{} ,heart beat", topic);
                    String msgId = UUID.randomUUID().toString();
                    HeartBeatDTO heartBeatDTO = new HeartBeatDTO();
                    heartBeatDTO.setMsgId(msgId);
                    TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.HEART_BEAT.getCode(), JSON.toJSONBytes(heartBeatDTO));
                    TcpMsg tcpMsgRes = nameServerNettyRemoteClient.sendSyncMsg(tcpMsg, msgId);
                    log.info("heart beat res: {}", JSON.toJSONString(tcpMsgRes));
                } catch (InterruptedException e) {
                    log.error("heart beat error:{}", e.getMessage(), e);
                }
            }
        }, "heart-beat-task");
        thread.start();
    }

    /**
     * 获取broker地址
     */
    public void fetchBrokerAddress() {
        String fetchBrokerAddressMsgId = UUID.randomUUID().toString();
        PullBrokerIpDTO pullBrokerIpDTO = new PullBrokerIpDTO();
        if (brokerClusterGroup != null) {
            brokerRole = "master";
            pullBrokerIpDTO.setBrokerClusterGroup(brokerClusterGroup);
        }
        pullBrokerIpDTO.setRole(brokerRole);
        pullBrokerIpDTO.setMsgId(fetchBrokerAddressMsgId);
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.PULL_BROKER_IP_LIST.getCode(), JSON.toJSONBytes(pullBrokerIpDTO));
        TcpMsg tcpMsgRes = nameServerNettyRemoteClient.sendSyncMsg(tcpMsg, fetchBrokerAddressMsgId);
        PullBrokerIpRespDTO pullBrokerIpRespDTO = JSON.parseObject(tcpMsgRes.getBody(), PullBrokerIpRespDTO.class);
        brokerAddressList = pullBrokerIpRespDTO.getAddressList();
        masterAddressList = pullBrokerIpRespDTO.getMasterAddressList();
        slaveAddressList = pullBrokerIpRespDTO.getSlaveAddressList();
        log.info("fetch broker address:{}, master:{}, slave:{}", brokerAddressList, masterAddressList, slaveAddressList);
        this.connectBroker();
    }

    private void connectBroker() {
        List<String> brokerAddressList = new ArrayList<>();
        if ("single".equals(brokerRole)) {
            AssertUtils.isNotEmpty(this.getBrokerAddressList(), "broker address list is empty");
            brokerAddressList = this.getBrokerAddressList();
        } else if ("master".equals(brokerRole)) {
            AssertUtils.isNotEmpty(this.getMasterAddressList(), "master broker address list is empty");
            brokerAddressList = this.getMasterAddressList();
        } else if ("slave".equals(brokerRole)) {
            AssertUtils.isNotEmpty(this.getSlaveAddressList(), "slave broker address list is empty");
            brokerAddressList = this.getSlaveAddressList();
        }
        //判断之前是否有链接过目标地址，以及链接是否正常，如果链接正常则没必要重新链接，避免无意义的通讯中断情况发生
        List<BrokerNettyRemoteClient> newBrokerNettyRemoteClientList = new ArrayList<>();
        for (String brokerAddress : brokerAddressList) {
            BrokerNettyRemoteClient brokerNettyRemoteClient = brokerNettyRemoteClientMap.get(brokerAddress);
            if (brokerNettyRemoteClient == null) {
                //之前没有连接过，需要额外连接接入
                String[] brokerAddressArr = brokerAddress.split(":");
                BrokerNettyRemoteClient newBrokerNettyRemoteClient = new BrokerNettyRemoteClient(brokerAddressArr[0], Integer.parseInt(brokerAddressArr[1]));
                newBrokerNettyRemoteClient.buildConnection(new BrokerRemoteRespHandler(new EventBus("consumer-client-eventbus")));
                //新的连接通道建立
                newBrokerNettyRemoteClientList.add(newBrokerNettyRemoteClient);
            } else if (brokerNettyRemoteClient.isChannelActive()) {
                //连接正常，不需要重新连接
                newBrokerNettyRemoteClientList.add(brokerNettyRemoteClient);
                continue;
            }
            //到这里的就是连接中断的, 尝试重新连接
            String[] brokerAddressArr = brokerAddress.split(":");
            BrokerNettyRemoteClient newBrokerNettyRemoteClient = new BrokerNettyRemoteClient(brokerAddressArr[0], Integer.parseInt(brokerAddressArr[1]));
            newBrokerNettyRemoteClient.buildConnection(new BrokerRemoteRespHandler(new EventBus("consumer-client-eventbus")));
            //添加到连接列表中
            newBrokerNettyRemoteClientList.add(newBrokerNettyRemoteClient);
        }
        //需要被关闭的链接过滤出来，进行优雅暂停，然后切换使用新的链接
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getBrokerRole() {
        return brokerRole;
    }

    public void setBrokerRole(String brokerRole) {
        this.brokerRole = brokerRole;
    }

    public Integer getQueueId() {
        return queueId;
    }

    public void setQueueId(Integer queueId) {
        this.queueId = queueId;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public String getBrokerClusterGroup() {
        return brokerClusterGroup;
    }

    public void setBrokerClusterGroup(String brokerClusterGroup) {
        this.brokerClusterGroup = brokerClusterGroup;
    }

    public NameServerNettyRemoteClient getNameServerNettyRemoteClient() {
        return nameServerNettyRemoteClient;
    }

    public void setNameServerNettyRemoteClient(NameServerNettyRemoteClient nameServerNettyRemoteClient) {
        this.nameServerNettyRemoteClient = nameServerNettyRemoteClient;
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

    public List<String> getSlaveAddressList() {
        return slaveAddressList;
    }

    public void setSlaveAddressList(List<String> slaveAddressList) {
        this.slaveAddressList = slaveAddressList;
    }

    public MessageConsumerListener getMessageConsumerListener() {
        return messageConsumerListener;
    }

    public void setMessageConsumerListener(MessageConsumerListener messageConsumerListener) {
        this.messageConsumerListener = messageConsumerListener;
    }

    public Map<String, BrokerNettyRemoteClient> getBrokerNettyRemoteClientMap() {
        return brokerNettyRemoteClientMap;
    }

    public void setBrokerNettyRemoteClientMap(Map<String, BrokerNettyRemoteClient> brokerNettyRemoteClientMap) {
        this.brokerNettyRemoteClientMap = brokerNettyRemoteClientMap;
    }
}
