package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerEventCode;
import org.gold.event.Listener;
import org.gold.event.model.ReplicationMsgEvent;
import org.gold.event.model.SlaveReplicationMsgAckEvent;
import org.gold.store.ServiceInstance;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public class SlaveReplicationMsgListener implements Listener<ReplicationMsgEvent> {
    @Override
    public void onReceive(ReplicationMsgEvent event) throws Exception {
        //从节点接收主节点的同步数据，成功接收后返回ack通知主节点
        ServiceInstance serviceInstance = event.getServiceInstance();
        //注册的实例[producer,consumer,broker]放入本地缓存
        CommonCache.getServiceInstanceManager().put(serviceInstance);
        SlaveReplicationMsgAckEvent slaveReplicationMsgAckEvent = new SlaveReplicationMsgAckEvent();
        slaveReplicationMsgAckEvent.setMsgId(event.getMsgId());
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.SLAVE_REPLICATION_ACK_MSG.getCode(), JSON.toJSONBytes(slaveReplicationMsgAckEvent));
        event.getChannelHandlerContext().writeAndFlush(tcpMsg);
    }
}
