package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.ConsumerMsgAckReqDTO;
import org.gold.dto.ConsumerMsgAckRespDTO;
import org.gold.enums.AckStatus;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.ConsumerMsgAckEvent;
import org.gold.model.GoldMqTopicModel;
import org.gold.rebalance.ConsumerInstance;

import java.util.List;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/11/19
 */
public class ConsumerMsgAckListener implements Listener<ConsumerMsgAckEvent> {

    private static final Logger log = LogManager.getLogger(ConsumerMsgAckListener.class);

    @Override
    public void onReceive(ConsumerMsgAckEvent event) throws Exception {
        ConsumerMsgAckReqDTO consumerMsgAckReqDTO = event.getConsumerMsgAckReqDTO();
        String topic = consumerMsgAckReqDTO.getTopic();
        String consumeGroup = consumerMsgAckReqDTO.getConsumeGroup();
        Integer queueId = consumerMsgAckReqDTO.getQueueId();
        Integer ackCount = consumerMsgAckReqDTO.getAckCount();
        //构建响应
        ConsumerMsgAckRespDTO consumerMsgAckRespDTO = new ConsumerMsgAckRespDTO();
        consumerMsgAckRespDTO.setMsgId(event.getMsgId());
        //获取topic信息
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        if (goldMqTopicModel == null) {
            //topic不存在,ack响应失败
            consumerMsgAckRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.BROKER_UPDATE_CONSUME_OFFSET_RESP.getCode(), JSON.toJSONBytes(consumerMsgAckRespDTO));
            event.getChannelHandlerContext().writeAndFlush(tcpMsg);
            return;
        }
        //存在topic
        Map<String, List<ConsumerInstance>> consumerInstanceMap = CommonCache.getConsumerHoldMap().get(topic);
        if (consumerInstanceMap == null || consumerInstanceMap.isEmpty()) {
            //topic没有消费者,ack响应失败
            consumerMsgAckRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.BROKER_UPDATE_CONSUME_OFFSET_RESP.getCode(), JSON.toJSONBytes(consumerMsgAckRespDTO));
            event.getChannelHandlerContext().writeAndFlush(tcpMsg);
            return;
        }
        //获取该消费者对应在broker端缓存的消费实例数据
        List<ConsumerInstance> consumerInstances = consumerInstanceMap.get(consumeGroup);
        if (CollectionUtils.isEmpty(consumerInstances)) {
            //消费实例信息不存在,ack响应失败
            consumerMsgAckRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.BROKER_UPDATE_CONSUME_OFFSET_RESP.getCode(), JSON.toJSONBytes(consumerMsgAckRespDTO));
            event.getChannelHandlerContext().writeAndFlush(tcpMsg);
            return;
        }
        String reqId = consumerMsgAckReqDTO.getIp() + ":" + consumerMsgAckReqDTO.getPort();
        ConsumerInstance matchConsumerInstance = consumerInstances.stream()
                .filter(instance -> instance.getConsumerReqId().equals(reqId))
                .findAny()
                .orElse(null);
        if (matchConsumerInstance == null) {
            //消费实例信息匹配不存在,ack响应失败
            consumerMsgAckRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.BROKER_UPDATE_CONSUME_OFFSET_RESP.getCode(), JSON.toJSONBytes(consumerMsgAckRespDTO));
            event.getChannelHandlerContext().writeAndFlush(tcpMsg);
            return;
        }
        for (int i = 0; i < ackCount; i++) {
            CommonCache.getConsumerQueueConsumeHandler().ack(topic, consumeGroup, queueId);
        }
        log.info("broker receive ack req, topic:{}, consumerGroup:{}, queueId:{}, ackCount:{}", topic, consumeGroup, queueId, ackCount);
        consumerMsgAckRespDTO.setAckStatus(AckStatus.SUCCESS.getCode());
        TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.BROKER_UPDATE_CONSUME_OFFSET_RESP.getCode(), JSON.toJSONBytes(consumerMsgAckRespDTO));
        event.getChannelHandlerContext().writeAndFlush(tcpMsg);
    }
}
