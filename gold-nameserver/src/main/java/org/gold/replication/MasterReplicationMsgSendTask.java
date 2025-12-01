package org.gold.replication;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 主从同步专用的数据发送任务
 */
public class MasterReplicationMsgSendTask extends ReplicationTask {
    public MasterReplicationMsgSendTask(String taskName) {
        super(taskName);
    }

    @Override
    public void startTask() {

    }
}
