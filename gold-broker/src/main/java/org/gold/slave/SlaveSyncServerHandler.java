package org.gold.slave;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.common.BrokerServerSyncFutureManager;
import org.gold.dto.MessageDTO;
import org.gold.dto.StartSyncRespDTO;
import org.gold.enums.BrokerEventCode;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.EventBus;
import org.gold.event.model.Event;
import org.gold.event.model.PushMsgEvent;
import org.gold.remote.BrokerServerSyncFuture;

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
        if (BrokerResponseCode.START_SYNC_SUCCESS.getCode() == code) {
            StartSyncRespDTO startSyncRespDTO = JSON.parseObject(body, StartSyncRespDTO.class);
            BrokerServerSyncFuture syncFuture = BrokerServerSyncFutureManager.getSyncFuture(startSyncRespDTO.getMsgId());
            if (syncFuture != null) {
                syncFuture.setResponse(startSyncRespDTO);
            }
        } else if (BrokerEventCode.PUSH_MSG.getCode() == code) {
            //这里是主节点同步消息过来
            MessageDTO messageDTO = JSON.parseObject(body, MessageDTO.class);
            PushMsgEvent pushMsgEvent = new PushMsgEvent();
            pushMsgEvent.setMessageDTO(messageDTO);
            pushMsgEvent.setMsgId(messageDTO.getMsgId());
            log.info("receive push msg：{}", JSON.toJSONString(messageDTO));
            event = pushMsgEvent;
            event.setChannelHandlerContext(ctx);
            eventBus.publish(event);
        }
    }
}
