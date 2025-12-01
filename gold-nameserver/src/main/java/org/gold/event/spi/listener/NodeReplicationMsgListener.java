package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.config.TraceReplicationProperties;
import org.gold.enums.NameServerEventCode;
import org.gold.event.Listener;
import org.gold.event.model.NodeReplicationAckMsgEvent;
import org.gold.event.model.NodeReplicationMsgEvent;
import org.gold.event.model.ReplicationMsgEvent;
import org.gold.store.ServiceInstance;

import java.net.Inet4Address;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public class NodeReplicationMsgListener implements Listener<NodeReplicationMsgEvent> {

    private static final Logger log = LogManager.getLogger(NodeReplicationMsgListener.class);

    @Override
    public void onReceive(NodeReplicationMsgEvent event) throws Exception {
        ServiceInstance serviceInstance = event.getServiceInstance();
        //接收前节点传递过来的数据，保存到本地缓存中
        CommonCache.getServiceInstanceManager().put(serviceInstance);
        ReplicationMsgEvent replicationMsgEvent = new ReplicationMsgEvent();
        replicationMsgEvent.setMsgId(event.getMsgId());
        replicationMsgEvent.setServiceInstance(serviceInstance);
        replicationMsgEvent.setType(event.getType());
        log.info("Received data from the previous node:{}", JSON.toJSONString(replicationMsgEvent));
        CommonCache.getReplicationMsgQueueManager().put(replicationMsgEvent);
        TraceReplicationProperties traceReplicationProperties = CommonCache.getNameserverProperties().getTraceReplicationProperties();
        if (StringUtil.isNullOrEmpty(traceReplicationProperties.getNextNode())) {
            //如果是尾部节点，则不需要再往下传递，但是需要返回给上一个节点ack消息
            log.info("This is the last node, no need to pass it on, but need to return to the previous node ack message!");
            NodeReplicationAckMsgEvent nodeReplicationAckMsgEvent = new NodeReplicationAckMsgEvent();
            nodeReplicationAckMsgEvent.setNodeIp(Inet4Address.getLocalHost().getHostAddress());
            nodeReplicationAckMsgEvent.setNodePort(traceReplicationProperties.getPort());
            nodeReplicationAckMsgEvent.setType(replicationMsgEvent.getType());
            nodeReplicationAckMsgEvent.setMsgId(replicationMsgEvent.getMsgId());
            TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.NODE_REPLICATION_ACK_MSG.getCode(), JSON.toJSONBytes(nodeReplicationAckMsgEvent));
            CommonCache.getPreNodeChannel().writeAndFlush(tcpMsg);
        }
    }
}
