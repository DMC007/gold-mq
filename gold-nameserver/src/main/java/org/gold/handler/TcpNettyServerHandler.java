package org.gold.handler;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.dto.HeartBeatDTO;
import org.gold.dto.ServiceRegistryReqDTO;
import org.gold.enums.NameServerEventCode;
import org.gold.event.EventBus;
import org.gold.event.model.Event;
import org.gold.event.model.HeartBeatEvent;
import org.gold.event.model.RegistryEvent;

import java.net.InetSocketAddress;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
@ChannelHandler.Sharable
public class TcpNettyServerHandler extends SimpleChannelInboundHandler<TcpMsg> {

    private static final Logger log = LogManager.getLogger(TcpNettyServerHandler.class);

    private EventBus eventBus;

    public TcpNettyServerHandler(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.init();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMsg msg) throws Exception {
        log.info("receive msg: {}", msg);
        int code = msg.getCode();
        byte[] body = msg.getBody();
        Event event = null;
        if (NameServerEventCode.REGISTRY.getCode() == code) {
            ServiceRegistryReqDTO serviceRegistryReqDTO = JSON.parseObject(body, ServiceRegistryReqDTO.class);
            RegistryEvent registryEvent = new RegistryEvent();
            registryEvent.setMsgId(serviceRegistryReqDTO.getMsgId());
            registryEvent.setRegistryType(serviceRegistryReqDTO.getRegistryType());
            registryEvent.setUser(serviceRegistryReqDTO.getUser());
            registryEvent.setPassword(serviceRegistryReqDTO.getPassword());
            registryEvent.setAttrs(serviceRegistryReqDTO.getAttrs());
            if (StringUtil.isNullOrEmpty(serviceRegistryReqDTO.getIp())) {
                InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                registryEvent.setIp(socketAddress.getHostString());
                registryEvent.setPort(socketAddress.getPort());
            } else {
                registryEvent.setIp(serviceRegistryReqDTO.getIp());
                registryEvent.setPort(serviceRegistryReqDTO.getPort());
            }
            event = registryEvent;
        } else if (NameServerEventCode.UN_REGISTRY.getCode() == code) {
        } else if (NameServerEventCode.HEART_BEAT.getCode() == code) {
            HeartBeatDTO heartBeatDTO = JSON.parseObject(body, HeartBeatDTO.class);
            HeartBeatEvent heartBeatEvent = new HeartBeatEvent();
            heartBeatEvent.setMsgId(heartBeatDTO.getMsgId());
            event = heartBeatEvent;
        } else {
            ctx.close();
            throw new RuntimeException("unsupported events");
        }
        event.setChannelHandlerContext(ctx);
        eventBus.publish(event);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel inactive");
        super.channelInactive(ctx);
    }
}
