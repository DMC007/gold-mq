package org.gold.event.spi.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.event.Listener;
import org.gold.event.model.SlaveHeartBeatEvent;

import java.net.InetSocketAddress;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 接收nameserver从节点心跳处理逻辑
 */
public class SlaveHeartBeatListener implements Listener<SlaveHeartBeatEvent> {

    private static final Logger log = LogManager.getLogger(SlaveHeartBeatListener.class);

    @Override
    public void onReceive(SlaveHeartBeatEvent event) throws Exception {
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress) event.getChannelHandlerContext().channel().remoteAddress();
            log.info("slave heart beat handler event:{},{}", remoteAddress.getAddress(), remoteAddress.getPort());
        } catch (Exception e) {
            log.error("slave heart beat handler error", e);
        }
    }
}
