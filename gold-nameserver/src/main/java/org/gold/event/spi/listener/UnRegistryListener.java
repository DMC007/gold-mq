package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerResponseCode;
import org.gold.event.Listener;
import org.gold.event.model.UnRegistryEvent;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public class UnRegistryListener implements Listener<UnRegistryEvent> {

    private static final Logger log = LogManager.getLogger(UnRegistryListener.class);

    @Override
    public void onReceive(UnRegistryEvent event) throws IllegalAccessException {
        ChannelHandlerContext ctx = event.getChannelHandlerContext();
        Object reqId = ctx.channel().attr(AttributeKey.valueOf("reqId")).get();
        if (reqId == null) {
            TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.ERROR_USER_OR_PASSWORD.getCode(),
                    NameServerResponseCode.ERROR_USER_OR_PASSWORD.getDesc().getBytes());
            ctx.writeAndFlush(tcpMsg);
            ctx.close();
            throw new IllegalAccessException("Authentication failed");
        }
        log.info("UnRegistryEvent:{}", JSON.toJSONString(event));
        String reqIdStr = reqId.toString();
        //移除需要下线的节点信息
        CommonCache.getServiceInstanceManager().remove(reqIdStr);
        //发送正常下线操作给客户端
        TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.UN_REGISTRY_SERVICE.getCode(),
                NameServerResponseCode.UN_REGISTRY_SERVICE.getDesc().getBytes());
        ctx.writeAndFlush(tcpMsg);
        //关闭连接
        ctx.close();
    }
}
