package org.gold.handler;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerEventCode;
import org.gold.event.EventBus;
import org.gold.event.model.Event;
import org.gold.event.model.NodeReplicationAckMsgEvent;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
@ChannelHandler.Sharable
public class NodeSendReplicationMsgServerHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private EventBus eventBus;

    public NodeSendReplicationMsgServerHandler(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.init();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {
        byte[] body = msg.getBody();
        int code = msg.getCode();
        Event event = null;
        if (code == NameServerEventCode.NODE_REPLICATION_ACK_MSG.getCode()) {
            event = JSON.parseObject(body, NodeReplicationAckMsgEvent.class);
        }
        if (event != null) {
            event.setChannelHandlerContext(ctx);
            eventBus.publish(event);
        }
    }
}
