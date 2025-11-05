package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.ServiceRegistryResDTO;
import org.gold.enums.NameServerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.RegistryEvent;
import org.gold.store.ServiceInstance;
import org.gold.utils.NameserverUtils;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public class RegistryListener implements Listener<RegistryEvent> {

    private static final Logger log = LogManager.getLogger(RegistryListener.class);

    @Override
    public void onReceive(RegistryEvent event) throws IllegalAccessException {
        boolean verify = NameserverUtils.isVerify(event.getUser(), event.getPassword());
        ChannelHandlerContext ctx = event.getChannelHandlerContext();
        if (!verify) {
            ServiceRegistryResDTO serviceRegistryResDTO = new ServiceRegistryResDTO();
            serviceRegistryResDTO.setMsgId(event.getMsgId());
            TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.ERROR_USER_OR_PASSWORD.getCode(),
                    JSON.toJSONBytes(serviceRegistryResDTO));
            ctx.writeAndFlush(tcpMsg);
            ctx.close();
            throw new IllegalAccessException("Authentication failed");
        }
        log.info("RegistryEvent:{}", JSON.toJSONString(event));
        ctx.channel().attr(AttributeKey.valueOf("reqId")).set(event.getIp() + ":" + event.getPort());
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setChannel(ctx.channel());
        serviceInstance.setIp(event.getIp());
        serviceInstance.setPort(event.getPort());
        serviceInstance.setRegistryType(event.getRegistryType());
        serviceInstance.setFirstRegistryTime(System.currentTimeMillis());
        serviceInstance.setAttrs(event.getAttrs());
        //serviceInstance.setLastHeartBeatTime(System.currentTimeMillis());
        //放入缓存
        CommonCache.getServiceInstanceManager().put(serviceInstance);
        //TODO 单机架构
        ServiceRegistryResDTO serviceRegistryResDTO = new ServiceRegistryResDTO();
        serviceRegistryResDTO.setMsgId(event.getMsgId());
        TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.REGISTRY_SUCCESS.getCode(), JSON.toJSONBytes(serviceRegistryResDTO));
        ctx.writeAndFlush(tcpMsg);
    }
}
