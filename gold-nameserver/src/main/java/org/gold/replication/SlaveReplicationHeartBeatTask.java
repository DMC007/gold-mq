package org.gold.replication;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 从节点给主节点发送心跳数据 定时任务
 */
public class SlaveReplicationHeartBeatTask extends ReplicationTask {

    public SlaveReplicationHeartBeatTask(String taskName) {
        super(taskName);
    }

    @Override
    public void startTask() {

    }
}
