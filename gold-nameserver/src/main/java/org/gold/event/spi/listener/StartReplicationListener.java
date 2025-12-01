package org.gold.event.spi.listener;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerEventCode;
import org.gold.enums.NameServerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.StartReplicationEvent;
import org.gold.utils.NameserverUtils;

import java.net.InetSocketAddress;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 开启同步复制监听器
 */
public class StartReplicationListener implements Listener<StartReplicationEvent> {

    @Override
    public void onReceive(StartReplicationEvent event) throws Exception {
        boolean verify = NameserverUtils.isVerify(event.getUser(), event.getPassword());
        ChannelHandlerContext ctx = event.getChannelHandlerContext();
        if (!verify) {
            TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.ERROR_USER_OR_PASSWORD.getCode(),
                    NameServerResponseCode.ERROR_USER_OR_PASSWORD.getDesc().getBytes());
            ctx.writeAndFlush(tcpMsg);
            ctx.close();
            throw new IllegalAccessException("Username or password incorrect!");
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        event.setSlaveIp(inetSocketAddress.getHostString());
        event.setSlavePort(String.valueOf(inetSocketAddress.getPort()));
        String reqId = event.getSlaveIp() + ":" + event.getSlavePort();
        //设置通道属性
        ctx.channel().attr(AttributeKey.valueOf("reqId")).set(reqId);
        CommonCache.getReplicationChannelManager().put(reqId, ctx);
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.MASTER_START_REPLICATION_ACK.getCode(), new byte[0]);
        ctx.writeAndFlush(tcpMsg);
    }
}
