package org.gold.nett.broker;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.dto.MessageDTO;
import org.gold.dto.StartSyncReqDTO;
import org.gold.enums.BrokerEventCode;
import org.gold.event.EventBus;
import org.gold.event.model.Event;
import org.gold.event.model.PushMsgEvent;
import org.gold.event.model.StartSyncEvent;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class BrokerServerHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private static final Logger log = LogManager.getLogger(BrokerServerHandler.class);

    private EventBus eventBus;

    public BrokerServerHandler(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.init();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {
        int code = msg.getCode();
        byte[] body = msg.getBody();
        Event event = null;
        if (BrokerEventCode.START_SYNC_MSG.getCode() == code) {
            StartSyncReqDTO startSyncReqDTO = JSON.parseObject(body, StartSyncReqDTO.class);
            StartSyncEvent startSyncEvent = new StartSyncEvent();
            startSyncEvent.setMsgId(startSyncReqDTO.getMsgId());
            event = startSyncEvent;
        } else if (BrokerEventCode.PUSH_MSG.getCode() == code) {
            MessageDTO messageDTO = JSON.parseObject(body, MessageDTO.class);
            PushMsgEvent pushMsgEvent = new PushMsgEvent();
            pushMsgEvent.setMessageDTO(messageDTO);
            pushMsgEvent.setMsgId(messageDTO.getMsgId());
            log.info("receive push msg：{}", JSON.toJSONString(messageDTO));
            event = pushMsgEvent;
        }
        if (event != null) {
            event.setChannelHandlerContext(ctx);
            eventBus.publish(event);
        }
    }
}
