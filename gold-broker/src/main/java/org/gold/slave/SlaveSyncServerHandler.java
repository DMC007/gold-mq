package org.gold.slave;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.event.EventBus;
import org.gold.event.model.Event;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
@ChannelHandler.Sharable
public class SlaveSyncServerHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private static final Logger log = LogManager.getLogger(SlaveSyncServerHandler.class);

    private EventBus eventBus;

    public SlaveSyncServerHandler(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.init();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {
        int code = msg.getCode();
        byte[] body = msg.getBody();
        Event event = null;
        //TODO
    }
}
