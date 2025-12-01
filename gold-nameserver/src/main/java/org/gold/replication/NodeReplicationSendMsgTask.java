package org.gold.replication;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.NodeAckDTO;
import org.gold.enums.NameServerEventCode;
import org.gold.event.model.NodeReplicationMsgEvent;
import org.gold.event.model.ReplicationMsgEvent;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 链式复制中，非尾部节点发送数据给下一个节点的任务
 */
public class NodeReplicationSendMsgTask extends ReplicationTask {

    private static final Logger logger = LogManager.getLogger(NodeReplicationSendMsgTask.class);

    public NodeReplicationSendMsgTask(String taskName) {
        super(taskName);
    }

    @Override
    public void startTask() {
        while (true) {
            try {
                //如果是头节点或者中间节点，则往下执行
                ReplicationMsgEvent replicationMsgEvent = CommonCache.getReplicationMsgQueueManager().getReplicationMsgQueue().take();
                //拿到当前节点，连接到下一个节点的通道
                Channel connectNodeChannel = CommonCache.getConnectNodeChannel();
                NodeReplicationMsgEvent nodeReplicationMsgEvent = new NodeReplicationMsgEvent();
                nodeReplicationMsgEvent.setMsgId(replicationMsgEvent.getMsgId());
                nodeReplicationMsgEvent.setServiceInstance(replicationMsgEvent.getServiceInstance());
                nodeReplicationMsgEvent.setType(replicationMsgEvent.getType());
                //当前任务的执行节点是否是头节点，是头节点就暂存客户端通道，等后续节点都响应ack后再回复客户端注册成功
                boolean isHeadNode = CommonCache.getPreNodeChannel() == null;
                if (isHeadNode) {
                    NodeAckDTO nodeAckDTO = new NodeAckDTO();
                    //broker的连接通道, 这里存储起来，等后续node节点都响应ack后，头节点能直接拿到这个值去给注册的客户端响应注册成功
                    nodeAckDTO.setChannelHandlerContext(replicationMsgEvent.getChannelHandlerContext());
                    CommonCache.getNodeAckMap().put(replicationMsgEvent.getMsgId(), nodeAckDTO);
                }
                if (connectNodeChannel.isActive()) {
                    TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.NODE_REPLICATION_MSG.getCode(), JSON.toJSONBytes(nodeReplicationMsgEvent));
                    //注意，这里的channel是前一个节点，去连接下一个节点的通道，我们这里发送消息，实际是下一个节点的自己的服务接收消息
                    connectNodeChannel.writeAndFlush(tcpMsg);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
