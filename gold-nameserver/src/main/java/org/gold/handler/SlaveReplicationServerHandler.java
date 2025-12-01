package org.gold.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;
import org.gold.event.EventBus;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
@ChannelHandler.Sharable
public class SlaveReplicationServerHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private EventBus eventBus;

    public SlaveReplicationServerHandler(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.init();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {

    }
}
