package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.core.CommitLogMMapFileModel;
import org.gold.dto.*;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.TimeWheelEvent;
import org.gold.model.TxMessageAckModel;
import org.gold.timewheel.DelayMessageDTO;
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
            } else if (SlotStoreTypeEnum.TX_MESSAGE_DTO.getClazz().equals(timeWheelSlotModel.getStoreType())) {
                TxMessageDTO txMessageDTO = (TxMessageDTO) timeWheelSlotModel.getData();
                //时间轮到期，检测ack缓存是否还有未提交剩余消息的ack记录
                TxMessageAckModel txMessageAckModel = CommonCache.getTxMessageAckModelMap().get(txMessageDTO.getMsgId());
                if (txMessageAckModel == null) {
                    //事务消息已经被ack
                    log.info("tx message ack model is null, msgId:{}", txMessageDTO.getMsgId());
                    continue;
                }
                //定时回调客户端
                TxMessageCallbackReqDTO txMessageCallbackReqDTO = new TxMessageCallbackReqDTO();
                txMessageCallbackReqDTO.setMessageDTO(txMessageAckModel.getMessageDTO());
                TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.TX_CALLBACK_MSG.getCode(), JSON.toJSONBytes(txMessageCallbackReqDTO));
                txMessageAckModel.getCtx().writeAndFlush(tcpMsg).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            log.info("tx message callback success");
                            //重新投递到时间轮[生产者收到回调请求响应后broker会删除txMessageAckModel, 就不会重复回调]
                            DelayMessageDTO delayMessageDTO = new DelayMessageDTO();
                            delayMessageDTO.setData(txMessageDTO);
                            delayMessageDTO.setSlotStoreType(SlotStoreTypeEnum.TX_MESSAGE_DTO);
                            delayMessageDTO.setNextExecuteTime(System.currentTimeMillis() + 3 * 1000L);
                            delayMessageDTO.setDelay(3);
                            CommonCache.getTimeWheelModelManager().add(delayMessageDTO);
                        } else {
                            //客户端异常
                            log.error("tx message callback error", future.cause());
                        }
                    }
                });
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
