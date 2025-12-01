package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.HeartBeatDTO;
import org.gold.enums.NameServerResponseCode;
import org.gold.enums.ReplicationMsgTypeEnum;
import org.gold.event.Listener;
import org.gold.event.model.HeartBeatEvent;
import org.gold.event.model.ReplicationMsgEvent;
import org.gold.store.ServiceInstance;

import java.util.UUID;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public class HeartBeatListener implements Listener<HeartBeatEvent> {

    private static final Logger log = LogManager.getLogger(HeartBeatListener.class);

    @Override
    public void onReceive(HeartBeatEvent event) throws IllegalAccessException {
        ChannelHandlerContext ctx = event.getChannelHandlerContext();
        Object reqId = ctx.channel().attr(AttributeKey.valueOf("reqId")).get();
        if (reqId == null) {
            TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.ERROR_USER_OR_PASSWORD.getCode(),
                    NameServerResponseCode.ERROR_USER_OR_PASSWORD.getDesc().getBytes());
            ctx.writeAndFlush(tcpMsg);
            ctx.close();
            throw new IllegalAccessException("Authentication failed");
        }
        log.info("HeartBeatEvent:{}", JSON.toJSONString(event));
        String reqIdStr = reqId.toString();
        String[] reqInfoStrArr = reqIdStr.split(":");
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setIp(reqInfoStrArr[0]);
        serviceInstance.setPort(Integer.parseInt(reqInfoStrArr[1]));
        serviceInstance.setLastHeartBeatTime(System.currentTimeMillis());
        //响应心跳请求
        HeartBeatDTO heartBeatDTO = new HeartBeatDTO();
        heartBeatDTO.setMsgId(event.getMsgId());
        TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.HEART_BEAT_SUCCESS.getCode(),
                JSON.toJSONString(heartBeatDTO).getBytes());
        ctx.writeAndFlush(tcpMsg);
        //异步发送无异常再放入本地缓存
        CommonCache.getServiceInstanceManager().putIfExist(serviceInstance);
        //如果nameserver架构是主从或者链路，还要给其他节点发送心跳消息
        ReplicationMsgEvent replicationMsgEvent = new ReplicationMsgEvent();
        replicationMsgEvent.setMsgId(UUID.randomUUID().toString());
        replicationMsgEvent.setServiceInstance(serviceInstance);
        replicationMsgEvent.setChannelHandlerContext(ctx);
        replicationMsgEvent.setType(ReplicationMsgTypeEnum.HEART_BEAT.getCode());
        //根据当前nameserver的角色，判断是否需要放入复制队列
        CommonCache.getReplicationMsgQueueManager().put(replicationMsgEvent);
    }
}
