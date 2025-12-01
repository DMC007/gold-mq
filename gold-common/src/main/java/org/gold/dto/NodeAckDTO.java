package org.gold.dto;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 链式复制中的ack对象
 */
public class NodeAckDTO {
    private ChannelHandlerContext channelHandlerContext;

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }
}
