package org.gold.store;

import io.netty.util.internal.StringUtil;
import org.gold.cache.CommonCache;
import org.gold.enums.ReplicationModeEnum;
import org.gold.enums.ReplicationRoleEnum;
import org.gold.event.model.ReplicationMsgEvent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public class ReplicationMsgQueueManager {
    private BlockingQueue<ReplicationMsgEvent> replicationMsgQueue = new ArrayBlockingQueue(5000);

    public BlockingQueue<ReplicationMsgEvent> getReplicationMsgQueue() {
        return replicationMsgQueue;
    }

    public void put(ReplicationMsgEvent replicationMsgEvent) {
        ReplicationModeEnum replicationModeEnum = ReplicationModeEnum.getByCode(CommonCache.getNameserverProperties().getReplicationMode());
        if (replicationModeEnum == null) {
            //单机架构，不做复制处理
            return;
        }
        if (replicationModeEnum == ReplicationModeEnum.MASTER_SLAVE) {
            ReplicationRoleEnum roleEnum = ReplicationRoleEnum.getByCode(CommonCache.getNameserverProperties().getMasterSlaveReplicationProperties().getRole());
            if (roleEnum != ReplicationRoleEnum.MASTER) {
                return;
            }
            this.sendMsgToQueue(replicationMsgEvent);
        } else if (replicationModeEnum == ReplicationModeEnum.TRACE) {
            String nextNode = CommonCache.getNameserverProperties().getTraceReplicationProperties().getNextNode();
            if (!StringUtil.isNullOrEmpty(nextNode)) {
                this.sendMsgToQueue(replicationMsgEvent);
            }
        }
    }

    private void sendMsgToQueue(ReplicationMsgEvent replicationMsgEvent) {
        try {
            replicationMsgQueue.put(replicationMsgEvent);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
