package org.gold.model;

import io.netty.channel.ChannelHandlerContext;
import org.gold.dto.MessageDTO;

/**
 * @author zhaoxun
 * @date 2025/11/25
 */
public class TxMessageAckModel {

    private MessageDTO messageDTO;

    private ChannelHandlerContext ctx;

    private long firstSendTime;

    public MessageDTO getMessageDTO() {
        return messageDTO;
    }

    public void setMessageDTO(MessageDTO messageDTO) {
        this.messageDTO = messageDTO;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public long getFirstSendTime() {
        return firstSendTime;
    }

    public void setFirstSendTime(long firstSendTime) {
        this.firstSendTime = firstSendTime;
    }
}
