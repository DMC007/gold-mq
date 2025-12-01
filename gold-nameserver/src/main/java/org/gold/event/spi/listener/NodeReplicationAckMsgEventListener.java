package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.NodeAckDTO;
import org.gold.enums.NameServerEventCode;
import org.gold.enums.NameServerResponseCode;
import org.gold.enums.ReplicationMsgTypeEnum;
import org.gold.event.Listener;
import org.gold.event.model.NodeReplicationAckMsgEvent;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public class NodeReplicationAckMsgEventListener implements Listener<NodeReplicationAckMsgEvent> {

    private static final Logger log = LogManager.getLogger(NodeReplicationAckMsgEventListener.class);

    @Override
    public void onReceive(NodeReplicationAckMsgEvent event) throws Exception {
        boolean isHeadNode = CommonCache.getPreNodeChannel() == null;
        if (isHeadNode) {
            log.info("This is the head node, no need to return ack message to the previous node!");
            //如果是头节点，响应给客户端整个链路已经同步复制完成
            NodeAckDTO nodeAckDTO = CommonCache.getNodeAckMap().get(event.getMsgId());
            //根据下游返回的msgId 获取对应的客户端连接到nameserver主节点的channel
            Channel clientRegistryChannel = nodeAckDTO.getChannelHandlerContext().channel();
            if (!clientRegistryChannel.isActive()) {
                // 如果客户端已经断开, 抛出异常
                try {
                    clientRegistryChannel.close();
                } catch (Exception e) {
                    log.error("clientRegistryChannel close error:{}", e.getMessage(), e);
                }
                throw new RuntimeException("clientRegistryChannel is not active!");
            }
            //移除nodeAckDTO对象
            CommonCache.getNodeAckMap().remove(event.getMsgId());
            if (ReplicationMsgTypeEnum.REGISTRY.getCode() == event.getType()) {
                TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.REGISTRY_SUCCESS.getCode(), NameServerResponseCode.REGISTRY_SUCCESS.getDesc().getBytes());
                clientRegistryChannel.writeAndFlush(tcpMsg);
            } else if (ReplicationMsgTypeEnum.HEART_BEAT.getCode() == event.getType()) {
                TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.HEART_BEAT_SUCCESS.getCode(), NameServerResponseCode.HEART_BEAT_SUCCESS.getDesc().getBytes());
                clientRegistryChannel.writeAndFlush(tcpMsg);
            }
        } else {
            log.info("This is not the head node, need to return ack message to the previous node!");
            //当前节点是中间节点，还得告知上一个节点同步完成
            CommonCache.getPreNodeChannel().writeAndFlush(new TcpMsg(NameServerEventCode.NODE_REPLICATION_ACK_MSG.getCode(), JSON.toJSONBytes(event)));
        }
    }
}
