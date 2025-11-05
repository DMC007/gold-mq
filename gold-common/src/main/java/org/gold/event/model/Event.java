package org.gold.event.model;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public abstract class Event {

    private String msgId;
    private ChannelHandlerContext channelHandlerContext;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }
}
