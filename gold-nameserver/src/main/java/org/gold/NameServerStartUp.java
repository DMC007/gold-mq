package org.gold;

import org.gold.cache.CommonCache;
import org.gold.core.InValidServiceRemoveTask;
import org.gold.core.NameServerStarter;
import org.gold.enums.ReplicationModeEnum;
import org.gold.replication.ReplicationService;

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
    }

    private static void initInvalidServerRemoveTask() {
        Thread inValidServiceRemoveTask = new Thread(new InValidServiceRemoveTask());
        inValidServiceRemoveTask.setName("invalid-server-remove-task");
        inValidServiceRemoveTask.start();
    }
}
