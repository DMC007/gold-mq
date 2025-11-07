package org.gold.nett.broker;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class BrokerServerHandler extends SimpleChannelInboundHandler<TcpMsg> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {

    }
}
