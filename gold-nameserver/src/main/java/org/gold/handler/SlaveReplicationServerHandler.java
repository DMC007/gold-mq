package org.gold.handler;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerEventCode;
import org.gold.event.EventBus;
import org.gold.event.model.Event;
import org.gold.event.model.ReplicationMsgEvent;

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
        int code = msg.getCode();
        byte[] body = msg.getBody();
        Event event = null;
        if (NameServerEventCode.MASTER_REPLICATION_MSG.getCode() == code) {
            event = JSON.parseObject(body, ReplicationMsgEvent.class);
        }
        if (event != null) {
            event.setChannelHandlerContext(ctx);
            eventBus.publish(event);
        }
    }
}
