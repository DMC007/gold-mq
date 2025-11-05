package org.gold.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
@ChannelHandler.Sharable
public class TcpNettyServerHandler extends SimpleChannelInboundHandler<TcpMsg> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {

    }
}
