package org.gold.consumer;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.dto.CreateTopicReqDTO;
import org.gold.enums.BrokerEventCode;
import org.gold.event.EventBus;
import org.gold.netty.BrokerRemoteRespHandler;
import org.gold.remote.BrokerNettyRemoteClient;

import java.util.UUID;

/**
 * @author zhaoxun
 * @date 2025/11/12
 */
public class CreateTopicClient {

    private static final Logger log = LogManager.getLogger(CreateTopicClient.class);

    public void createTopic(String topic, String brokerAddress) {
        String[] brokerAddr = brokerAddress.split(":");
        String ip = brokerAddr[0];
        Integer port = Integer.parseInt(brokerAddr[1]);
        //连接broker创建topic
        BrokerNettyRemoteClient brokerNettyRemoteClient = new BrokerNettyRemoteClient(ip, port);
        brokerNettyRemoteClient.buildConnection(new BrokerRemoteRespHandler(new EventBus("mq-client-eventbus")));
        String msgId = UUID.randomUUID().toString();
        CreateTopicReqDTO createTopicReqDTO = new CreateTopicReqDTO();
        createTopicReqDTO.setTopic(topic);
        //TODO 可改造成根据配置来
        createTopicReqDTO.setQueueSize(3);
        createTopicReqDTO.setMsgId(msgId);
        TcpMsg tcpMsg = new TcpMsg(BrokerEventCode.CREATE_TOPIC.getCode(), JSON.toJSONBytes(createTopicReqDTO));
        TcpMsg tcpMsgRes = brokerNettyRemoteClient.sendSyncMsg(tcpMsg, msgId);
        log.info("create topic res: {}", JSON.toJSONString(tcpMsgRes));
        brokerNettyRemoteClient.close();
    }
}
