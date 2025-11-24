package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.core.CommitLogMMapFileModel;
import org.gold.dto.ConsumerMsgCommitLogDTO;
import org.gold.dto.MessageDTO;
import org.gold.dto.MessageRetryDTO;
import org.gold.event.Listener;
import org.gold.event.model.TimeWheelEvent;
import org.gold.timewheel.SlotStoreTypeEnum;
import org.gold.timewheel.TimeWheelSlotModel;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author zhaoxun
 * @date 2025/11/24
 * @description 时间轮监听器
 */
public class TimeWheelListener implements Listener<TimeWheelEvent> {

    private static final Logger log = LogManager.getLogger(TimeWheelListener.class);

    @Override
    public void onReceive(TimeWheelEvent event) throws Exception {
        List<TimeWheelSlotModel> timeWheelSlotModelList = event.getTimeWheelSlotModelList();
        if (CollectionUtils.isEmpty(timeWheelSlotModelList)) {
            log.error("time wheel slot model list is empty");
            return;
        }
        for (TimeWheelSlotModel timeWheelSlotModel : timeWheelSlotModelList) {
            if (SlotStoreTypeEnum.MESSAGE_RETRY_DTO.getClazz().equals(timeWheelSlotModel.getStoreType())) {
                MessageRetryDTO messageRetryDTO = (MessageRetryDTO) timeWheelSlotModel.getData();
                this.messageRetryHandler(messageRetryDTO);
            } else if (SlotStoreTypeEnum.DELAY_MESSAGE_DTO.getClazz().equals(timeWheelSlotModel.getStoreType())) {
                MessageDTO messageDTO = (MessageDTO) timeWheelSlotModel.getData();
                log.info("delay message rewrite into commitLog:{}", JSON.toJSONString(messageDTO));
                //延迟消息重新写入commitLog，注意这里是message的topic是业务消息的topic,
                CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO, event);
            }
        }
    }

    /**
     * 消息重试处理器
     *
     * @param messageRetryDTO 消息重试DTO
     */
    private void messageRetryHandler(MessageRetryDTO messageRetryDTO) {
        CommitLogMMapFileModel commitLogMMapFileModel = CommonCache.getCommitLogMMapFileModelManager().get(messageRetryDTO.getTopic());
        ConsumerMsgCommitLogDTO consumerMsgCommitLogDTO = commitLogMMapFileModel.readContent(messageRetryDTO.getSourceCommitLogOffset(), messageRetryDTO.getSourceCommitLogSize());
        byte[] body = consumerMsgCommitLogDTO.getBody();
        log.info("send retry topic msg:{}", JSON.toJSONString(messageRetryDTO));
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setBody(body);
        messageDTO.setTopic("retry%" + messageRetryDTO.getConsumeGroup());
        messageDTO.setQueueId(ThreadLocalRandom.current().nextInt(3));
        messageDTO.setCurrentRetryTimes(messageRetryDTO.getCurrentRetryTimes() + 1);
        log.info("retryTimes：{}", messageDTO.getCurrentRetryTimes());
        try {
            CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
