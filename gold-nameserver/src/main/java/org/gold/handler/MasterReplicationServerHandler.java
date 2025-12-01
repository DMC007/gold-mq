package org.gold.handler;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerEventCode;
import org.gold.event.EventBus;
import org.gold.event.model.Event;
import org.gold.event.model.SlaveHeartBeatEvent;
import org.gold.event.model.SlaveReplicationMsgAckEvent;
import org.gold.event.model.StartReplicationEvent;

/**
 * @author zhaoxun
 * @date 2025/11/28
 */
@ChannelHandler.Sharable
public class MasterReplicationServerHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private EventBus eventBus;

    public MasterReplicationServerHandler(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.init();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {
        int code = msg.getCode();
        byte[] body = msg.getBody();
        Event event = null;
        if (code == NameServerEventCode.SLAVE_REPLICATION_ACK_MSG.getCode()) {
            event = JSON.parseObject(body, SlaveReplicationMsgAckEvent.class);
        } else if (code == NameServerEventCode.START_REPLICATION.getCode()) {
            event = JSON.parseObject(body, StartReplicationEvent.class);
        } else if (code == NameServerEventCode.SLAVE_HEART_BEAT.getCode()) {
            event = new SlaveHeartBeatEvent();
        }
        if (event != null) {
            event.setChannelHandlerContext(ctx);
            eventBus.publish(event);
        }
    }
}
