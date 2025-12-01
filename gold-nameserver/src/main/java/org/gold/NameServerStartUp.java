package org.gold;

import io.netty.util.internal.StringUtil;
import org.gold.cache.CommonCache;
import org.gold.core.InValidServiceRemoveTask;
import org.gold.core.NameServerStarter;
import org.gold.enums.ReplicationModeEnum;
import org.gold.enums.ReplicationRoleEnum;
import org.gold.replication.*;

import java.io.IOException;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class NameServerStartUp {

    private static NameServerStarter nameServerStarter;
    private static ReplicationService replicationService = new ReplicationService();

    public static void main(String[] args) throws IOException, InterruptedException {
        CommonCache.getPropertiesLoader().loadProperties();
        //获取到了集群复制的配置属性
        //master-slave 复制; trace 复制
        //如果是主从复制-》master角色-》开启一个额外的netty进程-》slave链接接入-》当数据写入master的时候，把写入的数据同步给到slave节点
        //如果是主从复制-》slave角色-》开启一个额外的netty进程-》slave端去链接master节点
        initReplication();
        initInvalidServerRemoveTask();
        nameServerStarter = new NameServerStarter(CommonCache.getNameserverProperties().getNameserverPort());
        nameServerStarter.startServer();
    }

    private static void initReplication() {
        //复制模式初始化
        ReplicationModeEnum replicationModeEnum = replicationService.checkProperties();
        //这里面会根据同步模式开启不同的netty进程
        replicationService.startReplicationTask(replicationModeEnum);
        ReplicationTask replicationTask = null;
        //开启定时任务
        if (replicationModeEnum == ReplicationModeEnum.MASTER_SLAVE) {
            ReplicationRoleEnum roleEnum = ReplicationRoleEnum.getByCode(CommonCache.getNameserverProperties().getMasterSlaveReplicationProperties().getRole());
            if (roleEnum == ReplicationRoleEnum.MASTER) {
                replicationTask = new MasterReplicationMsgSendTask("master-replication-msg-send-task");
                replicationTask.startTaskAsync();
            } else if (roleEnum == ReplicationRoleEnum.SLAVE) {
                replicationTask = new SlaveReplicationHeartBeatTask("slave-replication-heart-beat-send-task");
                replicationTask.startTaskAsync();
            }
        } else if (replicationModeEnum == ReplicationModeEnum.TRACE) {
            //判断当前节点是不是尾部节点，不是则开启一个复制数据的异步任务
            String nextNode = CommonCache.getNameserverProperties().getTraceReplicationProperties().getNextNode();
            if (!StringUtil.isNullOrEmpty(nextNode)) {
                replicationTask = new NodeReplicationSendMsgTask("node-replication-msg-send-task");
                replicationTask.startTaskAsync();
            }
        }
        CommonCache.setReplicationTask(replicationTask);
    }

    private static void initInvalidServerRemoveTask() {
        Thread inValidServiceRemoveTask = new Thread(new InValidServiceRemoveTask());
        inValidServiceRemoveTask.setName("invalid-server-remove-task");
        inValidServiceRemoveTask.start();
    }
}
