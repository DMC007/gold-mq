package org.gold.store;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public class ReplicationChannelManager {

    private static final Logger log = LogManager.getLogger(ReplicationChannelManager.class);
    private static Map<String, ChannelHandlerContext> channelHandlerContextMap = new ConcurrentHashMap<>();

    public static Map<String, ChannelHandlerContext> getChannelHandlerContextMap() {
        return channelHandlerContextMap;
    }

    public static void setChannelHandlerContextMap(Map<String, ChannelHandlerContext> channelHandlerContextMap) {
        ReplicationChannelManager.channelHandlerContextMap = channelHandlerContextMap;
    }

    public Map<String, ChannelHandlerContext> getValidSalveChannelMap() {
        List<String> inValidChannelReqIdList = new ArrayList<>();
        //判断当前采用的同步模式是哪种方式
        for (String reqId : channelHandlerContextMap.keySet()) {
            Channel slaveChannel = channelHandlerContextMap.get(reqId).channel();
            if (!slaveChannel.isActive()) {
                inValidChannelReqIdList.add(reqId);
                //关闭掉无用的通道
                try {
                    slaveChannel.close();
                } catch (Exception e) {
                    log.error("close channel error:{}", e.getMessage(), e);
                }
            }
        }
        if (!inValidChannelReqIdList.isEmpty()) {
            for (String reqId : inValidChannelReqIdList) {
                //移除不可用的channel
                channelHandlerContextMap.remove(reqId);
            }
        }
        return channelHandlerContextMap;
    }

    public void put(String reqId, ChannelHandlerContext channelHandlerContext) {
        channelHandlerContextMap.put(reqId, channelHandlerContext);
    }

    public ChannelHandlerContext get(String reqId) {
        return channelHandlerContextMap.get(reqId);
    }
}
