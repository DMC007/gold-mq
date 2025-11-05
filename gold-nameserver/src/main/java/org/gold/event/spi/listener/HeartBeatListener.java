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
import org.gold.event.Listener;
import org.gold.event.model.HeartBeatEvent;
import org.gold.store.ServiceInstance;

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
        CommonCache.getServiceInstanceManager().putIfExist(serviceInstance);
        HeartBeatDTO heartBeatDTO = new HeartBeatDTO();
        heartBeatDTO.setMsgId(event.getMsgId());
        TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.HEART_BEAT_SUCCESS.getCode(),
                JSON.toJSONString(heartBeatDTO).getBytes());
        ctx.writeAndFlush(tcpMsg);
    }
}
