package org.gold.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;
import org.gold.event.EventBus;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
@ChannelHandler.Sharable
public class BrokerRemoteRespHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private EventBus eventBus;

    public BrokerRemoteRespHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {

    }
}
