package org.gold.replication;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.enums.NameServerEventCode;
import org.gold.event.model.SlaveHeartBeatEvent;
import org.gold.event.model.StartReplicationEvent;

import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 从节点给主节点发送心跳数据 定时任务
 */
public class SlaveReplicationHeartBeatTask extends ReplicationTask {

    private static final Logger log = LogManager.getLogger(MasterReplicationMsgSendTask.class);

    public SlaveReplicationHeartBeatTask(String taskName) {
        super(taskName);
    }

    @Override
    public void startTask() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        StartReplicationEvent startReplicationEvent = new StartReplicationEvent();
        startReplicationEvent.setUser(CommonCache.getNameserverProperties().getNameserverUser());
        startReplicationEvent.setPassword(CommonCache.getNameserverProperties().getNameserverPwd());
        TcpMsg tcpMsg = new TcpMsg(NameServerEventCode.START_REPLICATION.getCode(), JSON.toJSONBytes(startReplicationEvent));
        //从节点连接主节点的时候，本地记录了channel，这里直接取出，然后通过channel给主节点发送消息
        CommonCache.getConnectNodeChannel().writeAndFlush(tcpMsg);
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(3);
                //发送数据给主节点
                TcpMsg slaveHeartBeatMsg = new TcpMsg(NameServerEventCode.SLAVE_HEART_BEAT.getCode(), JSON.toJSONBytes(new SlaveHeartBeatEvent()));
                //从节点连接主节点的时候，本地记录了channel，这里直接取出，然后通过channel给主节点发送心跳消息
                CommonCache.getConnectNodeChannel().writeAndFlush(slaveHeartBeatMsg);
            } catch (InterruptedException e) {
                log.error("slave replication heart beat task error", e);
            }
        }
    }
}
