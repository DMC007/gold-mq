package org.gold.replication;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 链式复制中，非尾部节点发送数据给下一个节点的任务
 */
public class NodeReplicationSendMsgTask extends ReplicationTask {

    public NodeReplicationSendMsgTask(String taskName) {
        super(taskName);
    }

    @Override
    public void startTask() {

    }
}
