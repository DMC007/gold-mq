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
import org.gold.enums.ReplicationModeEnum;
import org.gold.enums.ReplicationMsgTypeEnum;
import org.gold.event.Listener;
import org.gold.event.model.RegistryEvent;
import org.gold.event.model.ReplicationMsgEvent;
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
        ReplicationModeEnum replicationModeEnum = ReplicationModeEnum.getByCode(CommonCache.getNameserverProperties().getReplicationMode());
        if (replicationModeEnum == null) {
            // 单机架构, 直接返回注册成功
            ServiceRegistryResDTO serviceRegistryResDTO = new ServiceRegistryResDTO();
            serviceRegistryResDTO.setMsgId(event.getMsgId());
            TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.REGISTRY_SUCCESS.getCode(), JSON.toJSONBytes(serviceRegistryResDTO));
            ctx.writeAndFlush(tcpMsg);
            return;
        }
        ReplicationMsgEvent replicationMsgEvent = new ReplicationMsgEvent();
        replicationMsgEvent.setServiceInstance(serviceInstance);
        replicationMsgEvent.setChannelHandlerContext(ctx);
        replicationMsgEvent.setType(ReplicationMsgTypeEnum.REGISTRY.getCode());
        //如果当前为主从复制模式，而且当前角色是主节点，那么就往队列里面放入元素
        CommonCache.getReplicationMsgQueueManager().put(replicationMsgEvent);
        //同步给到从节点，比较严谨的同步，binlog类型，对于数据的顺序性要求很高了
        //可能是无顺序的状态
        //把同步的数据塞入一条队列当中，专门有一条线程从队列当中提取数据，同步给各个从节点
    }
}
