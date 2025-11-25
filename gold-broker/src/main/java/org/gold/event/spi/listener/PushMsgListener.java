package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.MessageDTO;
import org.gold.dto.SendMessageToBrokerResponseDTO;
import org.gold.dto.TxMessageDTO;
import org.gold.enums.BrokerResponseCode;
import org.gold.enums.MessageSendWay;
import org.gold.enums.SendMessageToBrokerResponseStatus;
import org.gold.enums.TxMessageFlagEnum;
import org.gold.event.Listener;
import org.gold.event.model.PushMsgEvent;
import org.gold.model.TxMessageAckModel;
import org.gold.timewheel.DelayMessageDTO;
import org.gold.timewheel.SlotStoreTypeEnum;
import org.gold.utils.AssertUtils;

import java.io.IOException;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
public class PushMsgListener implements Listener<PushMsgEvent> {
    @Override
    public void onReceive(PushMsgEvent event) throws Exception {
        //将消息写入commitLog
        MessageDTO messageDTO = event.getMessageDTO();
        //是否是延迟消息
        boolean isDelay = messageDTO.getDelay() > 0;
        //TODO 是否是事务消息
        boolean isHalfMsg = messageDTO.getTxFlag() == TxMessageFlagEnum.HALF_MSG.getCode();
        if (isDelay) {
            //延迟消息处理
            this.appendDelayMsgHandler(messageDTO, event);
        } else if (isHalfMsg) {
            //事务消息处理
            this.halfMsgHandler(messageDTO, event);
        } else {
            //普通消息处理[event里面的ctx需要用来做响应]
            this.appendDefaultMsgHandler(messageDTO, event);
        }
    }

    private void halfMsgHandler(MessageDTO messageDTO, PushMsgEvent event) {
        TxMessageAckModel txMessageAckModel = new TxMessageAckModel();
        txMessageAckModel.setMessageDTO(messageDTO);
        txMessageAckModel.setCtx(event.getChannelHandlerContext());
        txMessageAckModel.setFirstSendTime(System.currentTimeMillis());
        CommonCache.getTxMessageAckModelMap().put(messageDTO.getMsgId(), txMessageAckModel);
        //时间轮推送
        TxMessageDTO txMessageDTO = new TxMessageDTO();
        txMessageDTO.setMsgId(messageDTO.getMsgId());
        long currentTime = System.currentTimeMillis();
        DelayMessageDTO delayMessageDTO = new DelayMessageDTO();
        delayMessageDTO.setData(txMessageDTO);
        delayMessageDTO.setSlotStoreType(SlotStoreTypeEnum.TX_MESSAGE_DTO);
        delayMessageDTO.setNextExecuteTime(currentTime + 3 * 1000L);
        delayMessageDTO.setDelay(3);
        CommonCache.getTimeWheelModelManager().add(delayMessageDTO);
        //通知客户端写入事务消息成功
        SendMessageToBrokerResponseDTO sendMessageToBrokerResponseDTO = new SendMessageToBrokerResponseDTO();
        sendMessageToBrokerResponseDTO.setMsgId(messageDTO.getMsgId());
        sendMessageToBrokerResponseDTO.setStatus(SendMessageToBrokerResponseStatus.SUCCESS.getCode());
        sendMessageToBrokerResponseDTO.setDesc("send tx half message success");
        TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.HALF_MSG_SEND_SUCCESS.getCode(), JSON.toJSONBytes(sendMessageToBrokerResponseDTO));
        event.getChannelHandlerContext().writeAndFlush(tcpMsg);
    }

    /**
     * 延迟消息追加写入
     *
     * @param messageDTO 消息体
     * @param event      事件
     */
    private void appendDelayMsgHandler(MessageDTO messageDTO, PushMsgEvent event) throws IOException {
        int delay = messageDTO.getDelay();
        //延迟时间不能大于1小时
        AssertUtils.isTrue(delay <= 3600, "too large delay seconds");
        DelayMessageDTO delayMessageDTO = new DelayMessageDTO();
        delayMessageDTO.setData(messageDTO);
        delayMessageDTO.setDelay(delay);
        delayMessageDTO.setSlotStoreType(SlotStoreTypeEnum.DELAY_MESSAGE_DTO);
        delayMessageDTO.setNextExecuteTime(System.currentTimeMillis() + delay * 1000L);
        //写入延迟消息到时间轮中
        CommonCache.getTimeWheelModelManager().add(delayMessageDTO);
        //持久化
        MessageDTO delayMessage = new MessageDTO();
        delayMessage.setBody(JSON.toJSONBytes(delayMessageDTO));
        delayMessage.setTopic("delay_queue");
        delayMessage.setQueueId(0);
        delayMessage.setSendWay(MessageSendWay.ASYNC.getCode());
        CommonCache.getCommitLogAppendHandler().appendMessage(delayMessage, event);
        //响应客户端
        SendMessageToBrokerResponseDTO sendMessageToBrokerResponseDTO = new SendMessageToBrokerResponseDTO();
        sendMessageToBrokerResponseDTO.setMsgId(messageDTO.getMsgId());
        sendMessageToBrokerResponseDTO.setStatus(SendMessageToBrokerResponseStatus.SUCCESS.getCode());
        sendMessageToBrokerResponseDTO.setDesc("send delay message success");
        TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.SEND_MSG_RESP.getCode(), JSON.toJSONBytes(sendMessageToBrokerResponseDTO));
        event.getChannelHandlerContext().writeAndFlush(tcpMsg);
    }

    private void appendDefaultMsgHandler(MessageDTO messageDTO, PushMsgEvent event) throws IOException {
        CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO, event);
    }
}
