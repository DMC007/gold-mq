package org.gold.nett.broker;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.common.BrokerServerSyncFutureManager;
import org.gold.dto.*;
import org.gold.enums.BrokerEventCode;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.EventBus;
import org.gold.event.model.*;
import org.gold.remote.BrokerServerSyncFuture;

import java.net.InetSocketAddress;

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
        } else if (BrokerResponseCode.SLAVE_SYNC_RESP.getCode() == code) {
            SlaveSyncRespDTO slaveSyncRespDTO = JSON.parseObject(body, SlaveSyncRespDTO.class);
            BrokerServerSyncFuture syncFuture = BrokerServerSyncFutureManager.getSyncFuture(slaveSyncRespDTO.getMsgId());
            if (syncFuture != null) {
                syncFuture.setResponse(slaveSyncRespDTO);
            }
        } else if (BrokerEventCode.CONSUME_MSG.getCode() == code) {
            ConsumerMsgReqDTO consumerMsgReqDTO = JSON.parseObject(body, ConsumerMsgReqDTO.class);
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            consumerMsgReqDTO.setIp(remoteAddress.getHostString());
            consumerMsgReqDTO.setPort(remoteAddress.getPort());
            //定义事件
            ConsumerMsgEvent consumerMsgEvent = new ConsumerMsgEvent();
            consumerMsgEvent.setConsumerMsgReqDTO(consumerMsgReqDTO);
            consumerMsgEvent.setMsgId(consumerMsgReqDTO.getMsgId());
            //这里定义属性主要用来在CommonCache里面存储consumerInstance实例，故当消费组发生断开连接后CommonCache会删除对应的consumerInstance实例
            ctx.channel().attr(AttributeKey.valueOf("consumer-reqId")).set(consumerMsgReqDTO.getIp() + ":" + consumerMsgReqDTO.getPort());
            event = consumerMsgEvent;
        } else if (BrokerEventCode.CONSUME_SUCCESS_MSG.getCode() == code) {
            ConsumerMsgAckReqDTO consumerMsgAckReqDTO = JSON.parseObject(body, ConsumerMsgAckReqDTO.class);
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            consumerMsgAckReqDTO.setIp(remoteAddress.getHostString());
            consumerMsgAckReqDTO.setPort(remoteAddress.getPort());
            //定义事件
            ConsumerMsgAckEvent consumerMsgAckEvent = new ConsumerMsgAckEvent();
            consumerMsgAckEvent.setConsumerMsgAckReqDTO(consumerMsgAckReqDTO);
            consumerMsgAckEvent.setMsgId(consumerMsgAckReqDTO.getMsgId());
            event = consumerMsgAckEvent;
        } else if (BrokerEventCode.CONSUME_LATER_MSG.getCode() == code) {
            ConsumerMsgRetryReqDTO consumerMsgRetryReqDTO = JSON.parseObject(body, ConsumerMsgRetryReqDTO.class);
            //定义事件
            ConsumerMsgRetryEvent consumerMsgRetryEvent = new ConsumerMsgRetryEvent();
            consumerMsgRetryEvent.setConsumerMsgRetryReqDTO(consumerMsgRetryReqDTO);
            consumerMsgRetryEvent.setMsgId(consumerMsgRetryReqDTO.getMsgId());
            event = consumerMsgRetryEvent;
        }
        if (event != null) {
            event.setChannelHandlerContext(ctx);
            eventBus.publish(event);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel inactive：{}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
        Object reqId = ctx.channel().attr(AttributeKey.valueOf("consumer-reqId")).get();
        if (reqId == null) {
            return;
        }
        log.info("consumer disconnect：{}", reqId);
        CommonCache.getConsumerInstancePool().removeFromInstancePool(String.valueOf(reqId));
    }
}
