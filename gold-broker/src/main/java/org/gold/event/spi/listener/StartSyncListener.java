package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.StartSyncRespDTO;
import org.gold.enums.BrokerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.StartSyncEvent;

import java.net.InetSocketAddress;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description 开启同步监听器
 */
public class StartSyncListener implements Listener<StartSyncEvent> {
    @Override
    public void onReceive(StartSyncEvent event) throws Exception {
        ChannelHandlerContext ctx = event.getChannelHandlerContext();
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String reqId = remoteAddress.getAddress() + ":" + remoteAddress.getPort();
        ctx.channel().attr(AttributeKey.valueOf("reqId")).set(reqId);
        //保存从节点的channel到缓存对象
        CommonCache.getSlaveChannelMap().put(reqId, ctx);
        //处理完毕响应从节点
        StartSyncRespDTO startSyncRespDTO = new StartSyncRespDTO();
        startSyncRespDTO.setMsgId(event.getMsgId());
        startSyncRespDTO.setSuccess(true);
        TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.START_SYNC_SUCCESS.getCode(), JSON.toJSONBytes(startSyncRespDTO));
        ctx.writeAndFlush(tcpMsg);
    }
}
