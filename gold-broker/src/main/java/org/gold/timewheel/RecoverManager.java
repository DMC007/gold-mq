package org.gold.timewheel;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.dto.ConsumerMsgCommitLogDTO;
import org.gold.dto.MessageDTO;
import org.gold.enums.MessageSendWay;
import org.gold.model.ConsumerQueueConsumeReqModel;

import java.io.IOException;
import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/24
 * @description 时间轮恢复管理
 */
public class RecoverManager {

    private static final Logger log = LogManager.getLogger(RecoverManager.class);

    /**
     * 恢复延迟消息
     */
    public static void recoverDelayMessage() {
        log.info("start recover time wheel");
        //读取mmap中的数据内容
        String topic = "delay_queue";
        String consumeGroup = "broker_delay_message_recovery_job";
        Integer queueId = 0;
        ConsumerQueueConsumeReqModel consumerQueueConsumeReqModel = new ConsumerQueueConsumeReqModel();
        consumerQueueConsumeReqModel.setTopic(topic);
        consumerQueueConsumeReqModel.setConsumerGroup(consumeGroup);
        consumerQueueConsumeReqModel.setQueueId(queueId);
        //一条一条的恢复
        consumerQueueConsumeReqModel.setBatchSize(1);
        long currentTime = System.currentTimeMillis();
        while (true) {
            List<ConsumerMsgCommitLogDTO> commitLogDTOList = CommonCache.getConsumerQueueConsumeHandler().consume(consumerQueueConsumeReqModel);
            if (CollectionUtils.isEmpty(commitLogDTOList)) {
                log.info("no more data to recover");
                break;
            }
            ConsumerMsgCommitLogDTO consumerMsgCommitLogDTO = commitLogDTOList.getFirst();
            byte[] body = consumerMsgCommitLogDTO.getBody();
            //数据恢复需要考虑的问题：1. 不需要，已过期的数据可以考虑丢弃or直接扔到下一个slot. 2.delay需要重新计算
            DelayMessageDTO delayMessageDTO = JSON.parseObject(body, DelayMessageDTO.class);
            long nextExecuteTime = delayMessageDTO.getNextExecuteTime();
            if (nextExecuteTime <= currentTime) {
                //TODO 如果重新扔到文件【直接写入到对应业务topic的commitLog】，可能在宕机前该条数据已经消费过一次，可能会导致重复消费，所以需要消费者幂等处理
                tryInputToCommitLog(delayMessageDTO);
            } else {
                //还没过期，重新计算放入时间轮
                tryInputToTimeWheelAgain(delayMessageDTO);
            }
            log.info("recover delay message:{}", JSON.toJSONString(delayMessageDTO));
            //消息消费过后需要ack
            CommonCache.getConsumerQueueConsumeHandler().ack(topic, consumeGroup, queueId);
        }
    }

    private static void tryInputToTimeWheelAgain(DelayMessageDTO delayMessageDTO) {
        //计算出延迟时间相当于当前时间还差多少秒
        int remainTime = (int) ((delayMessageDTO.getNextExecuteTime() - System.currentTimeMillis()) / 1000);
        delayMessageDTO.setDelay(remainTime);
        CommonCache.getTimeWheelModelManager().add(delayMessageDTO);
    }

    private static void tryInputToCommitLog(DelayMessageDTO delayMessageDTO) {
        MessageDTO messageDTO = JSON.parseObject(JSON.toJSONString(delayMessageDTO.getData()), MessageDTO.class);
        messageDTO.setDelay(0);
        messageDTO.setSendWay(MessageSendWay.ASYNC.getCode());
        log.info("delay message try rewrite input to commitLog:{}", JSON.toJSONString(messageDTO));
        try {
            CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO, null);
        } catch (IOException e) {
            log.error("try input to commitLog error", e);
        }
    }
}
