package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.*;
import org.gold.enums.AckStatus;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.ConsumerMsgRetryEvent;
import org.gold.model.GoldMqTopicModel;
import org.gold.rebalance.ConsumerInstance;
import org.gold.timewheel.DelayMessageDTO;
import org.gold.timewheel.SlotStoreTypeEnum;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/11/20
 */
public class ConsumerMsgRetryListener implements Listener<ConsumerMsgRetryEvent> {

    private static final Logger log = LogManager.getLogger(ConsumerMsgRetryListener.class);

    private static final List<Integer> RETRY_STEP = Arrays.asList(3, 5, 10, 15, 30);

    @Override
    public void onReceive(ConsumerMsgRetryEvent event) throws Exception {
        log.info("consumer msg retry handler event:{}", JSON.toJSONString(event));
        ConsumerMsgRetryReqDTO consumerMsgRetryReqDTO = event.getConsumerMsgRetryReqDTO();
        String msgId = event.getMsgId();
        //定义响应
        ConsumerMsgRetryRespDTO consumerMsgRetryRespDTO = new ConsumerMsgRetryRespDTO();
        consumerMsgRetryRespDTO.setMsgId(msgId);
        InetSocketAddress remoteAddress = (InetSocketAddress) event.getChannelHandlerContext().channel().remoteAddress();
        List<ConsumerMsgRetryReqDetailDTO> consumerMsgRetryReqDetailDTOList = consumerMsgRetryReqDTO.getConsumerMsgRetryReqDetailDTOList();
        for (ConsumerMsgRetryReqDetailDTO consumerMsgRetryReqDetailDTO : consumerMsgRetryReqDetailDTOList) {
            consumerMsgRetryReqDetailDTO.setIp(remoteAddress.getHostString());
            consumerMsgRetryReqDetailDTO.setPort(remoteAddress.getPort());
            //如果参数异常，中间会抛出异常，不会继续后续的ack和重新发送topic
            this.checkParam(consumerMsgRetryRespDTO, consumerMsgRetryReqDetailDTO, event.getChannelHandlerContext());
        }
        for (ConsumerMsgRetryReqDetailDTO consumerMsgRetryReqDetailDTO : consumerMsgRetryReqDetailDTOList) {
            this.ackAndSendToRetryTopic(consumerMsgRetryReqDetailDTO, event);
        }
        //TODO 响应
        consumerMsgRetryRespDTO.setAckStatus(AckStatus.SUCCESS.getCode());
        TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RETRY_RESP.getCode(), JSON.toJSONBytes(consumerMsgRetryRespDTO));
        event.getChannelHandlerContext().writeAndFlush(tcpMsg);
    }

    private void ackAndSendToRetryTopic(ConsumerMsgRetryReqDetailDTO consumerMsgRetryReqDetailDTO, ConsumerMsgRetryEvent event) {
        String topic = consumerMsgRetryReqDetailDTO.getTopic();
        Integer queueId = consumerMsgRetryReqDetailDTO.getQueueId();
        String consumerGroup = consumerMsgRetryReqDetailDTO.getConsumerGroup();
        //这里的消息是重试的，重试是消费失败后消费端请求到这边broker处理，那么broker先把这个请求当作ack，
        // 这样可以保证后续的消息能够正常处理，避免因为一个消息重试导致后续消息大量堆积
        CommonCache.getConsumerQueueConsumeHandler().ack(topic, consumerGroup, queueId);
        //到这里，把需要重试的消息offset地址存储到retry主题里面，当时间到了之后，重新取出推到重试队列的专用主题中
        //这里借鉴rocketmq的实现思想，rocketmq消费失败或者延迟消息都会先投递到SCHEDULE_TOPIC_XXXX主题，后续等时间到了再分发到对应主题队列
        //注意：rocketmq5.0已经不使用SCHEDULE_TOPIC_XXXX方式，而是采用时间轮
        Integer commitLogMsgLength = consumerMsgRetryReqDetailDTO.getCommitLogMsgLength();
        Long commitLogOffset = consumerMsgRetryReqDetailDTO.getCommitLogOffset();
        MessageRetryDTO messageRetryDTO = new MessageRetryDTO();
        messageRetryDTO.setTopic(topic);
        messageRetryDTO.setQueueId(queueId);
        messageRetryDTO.setConsumeGroup(consumerGroup);
        messageRetryDTO.setSourceCommitLogOffset(Math.toIntExact(commitLogOffset));
        messageRetryDTO.setSourceCommitLogSize(commitLogMsgLength);
        messageRetryDTO.setCurrentRetryTimes(consumerMsgRetryReqDetailDTO.getRetryTime());
        //定义commitLog提交对象，用于存储重试信息
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setMsgId(event.getMsgId());
        //塞入重试信息的dto对象的字节数组
        messageDTO.setBody(JSON.toJSONBytes(messageRetryDTO));
        messageDTO.setRetry(true);
        //获取对应的重试级别
        Integer nextRetryTimeStep = RETRY_STEP.get(consumerMsgRetryReqDetailDTO.getRetryTime());
        try {
            if (nextRetryTimeStep == null) {
                //超过重试次数上限，写入死信队列：dead_queue的commitLog文件
                messageDTO.setTopic("dead_queue");
                //写入死信队列topic的commitLog
                CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO);
            } else {
                //这里的retry队列，理解为SCHEDULE_TOPIC_XXXX主题，后续等时间到了再分发到对应重试主题队列[消费者启动的时候会启动自己的消费组，还有重试队列的消费组]
                messageDTO.setTopic("retry");
                //写入重试队列的commitLog
                CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO);
                //定义重试级别
                long nextRetryTime = System.currentTimeMillis() + (nextRetryTimeStep * 1000);
                messageRetryDTO.setNextRetryTime(nextRetryTime);
                //重试消息放入时间轮，实现消息延迟重试效果
                DelayMessageDTO delayMessageDTO = new DelayMessageDTO();
                delayMessageDTO.setData(messageRetryDTO);
                delayMessageDTO.setSlotStoreType(SlotStoreTypeEnum.MESSAGE_RETRY_DTO);
                delayMessageDTO.setDelay(nextRetryTimeStep);
                delayMessageDTO.setNextExecuteTime(System.currentTimeMillis() + nextRetryTimeStep * 1000);
                CommonCache.getTimeWheelModelManager().add(delayMessageDTO);
            }
        } catch (Exception e) {
            log.error("retry message error", e);
            throw new RuntimeException(e);
        }
    }

    private void checkParam(ConsumerMsgRetryRespDTO consumerMsgRetryRespDTO,
                            ConsumerMsgRetryReqDetailDTO consumerMsgRetryReqDetailDTO, ChannelHandlerContext ctx) {
        String topic = consumerMsgRetryReqDetailDTO.getTopic();
        String consumerGroup = consumerMsgRetryReqDetailDTO.getConsumerGroup();
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        if (goldMqTopicModel == null) {
            consumerMsgRetryRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RETRY_RESP.getCode(), JSON.toJSONBytes(consumerMsgRetryRespDTO));
            ctx.writeAndFlush(tcpMsg);
            throw new RuntimeException("topic not exist");
        }
        Map<String, List<ConsumerInstance>> consumerInstanceMap = CommonCache.getConsumerHoldMap().get(topic);
        if (consumerInstanceMap == null || consumerInstanceMap.isEmpty()) {
            consumerMsgRetryRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RETRY_RESP.getCode(), JSON.toJSONBytes(consumerMsgRetryRespDTO));
            ctx.writeAndFlush(tcpMsg);
            throw new RuntimeException("consumer instance not exist");
        }
        List<ConsumerInstance> consumerInstances = consumerInstanceMap.get(consumerGroup);
        if (CollectionUtils.isEmpty(consumerInstances)) {
            consumerMsgRetryRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RETRY_RESP.getCode(), JSON.toJSONBytes(consumerMsgRetryRespDTO));
            ctx.writeAndFlush(tcpMsg);
            throw new RuntimeException("consumer instance not exist");
        }
        String reqId = consumerMsgRetryReqDetailDTO.getIp() + ":" + consumerMsgRetryReqDetailDTO.getPort();
        ConsumerInstance consumerInstance = consumerInstances.stream()
                .filter(e -> reqId.equals(e.getConsumerReqId()))
                .findAny()
                .orElse(null);
        if (consumerInstance == null) {
            consumerMsgRetryRespDTO.setAckStatus(AckStatus.FAIL.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.CONSUME_MSG_RETRY_RESP.getCode(), JSON.toJSONBytes(consumerMsgRetryRespDTO));
            ctx.writeAndFlush(tcpMsg);
            throw new RuntimeException("consumer instance not exist");
        }
    }
}
