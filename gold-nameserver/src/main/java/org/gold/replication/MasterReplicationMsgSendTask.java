package org.gold.replication;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.config.MasterSlaveReplicationProperties;
import org.gold.dto.SlaveAckDTO;
import org.gold.enums.MasterSlaveReplicationTypeEnum;
import org.gold.enums.NameServerEventCode;
import org.gold.enums.NameServerResponseCode;
import org.gold.event.model.ReplicationMsgEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaoxun
 * @date 2025/12/1
 * @description 主从同步专用的数据发送任务
 */
public class MasterReplicationMsgSendTask extends ReplicationTask {

    private static final Logger log = LogManager.getLogger(MasterReplicationMsgSendTask.class);

    public MasterReplicationMsgSendTask(String taskName) {
        super(taskName);
    }

    @Override
    public void startTask() {
        MasterSlaveReplicationProperties masterSlaveReplicationProperties = CommonCache.getNameserverProperties().getMasterSlaveReplicationProperties();
        MasterSlaveReplicationTypeEnum replicationTypeEnum = MasterSlaveReplicationTypeEnum.getByCode(masterSlaveReplicationProperties.getType());
        //判断当前的复制模式
        //如果是异步复制，直接发送同步数据，同时返回注册成功信号给到broker节点
        //如果是同步复制，发送同步数据给到slave节点，slave节点返回ack信号，主节点收到ack信号后通知给broker注册成功
        //半同步复制其实和同步复制思路很相似
        while (true) {
            try {
                ReplicationMsgEvent replicationMsgEvent = CommonCache.getReplicationMsgQueueManager().getReplicationMsgQueue().take();
                Channel brokerChannel = replicationMsgEvent.getChannelHandlerContext().channel();
                Map<String, ChannelHandlerContext> channelHandlerContextMap = CommonCache.getReplicationChannelManager().getValidSalveChannelMap();
                int validSlaveChannelCount = channelHandlerContextMap.keySet().size();
                if (replicationTypeEnum == MasterSlaveReplicationTypeEnum.ASYNC) {
                    this.sendMsgToSlave(replicationMsgEvent);
                    TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.REGISTRY_SUCCESS.getCode(), NameServerResponseCode.REGISTRY_SUCCESS.getDesc().getBytes());
                    brokerChannel.writeAndFlush(tcpMsg);
                } else if (replicationTypeEnum == MasterSlaveReplicationTypeEnum.SYNC) {
                    //需要接收多少个ack的次数
                    this.inputMsgToAckMap(replicationMsgEvent, validSlaveChannelCount);
                    this.sendMsgToSlave(replicationMsgEvent);
                } else if (replicationTypeEnum == MasterSlaveReplicationTypeEnum.HALF_SYNC) {
                    this.inputMsgToAckMap(replicationMsgEvent, validSlaveChannelCount / 2);
                    this.sendMsgToSlave(replicationMsgEvent);
                }
            } catch (Exception e) {
                log.error("master replication task error", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 将主节点需要发送出去的数据注入到一个map中，然后当从节点返回ack的时候，该map的数据会被剔除对应记录
     *
     * @param replicationMsgEvent 主节点需要发送的数据
     * @param needAckCount        从节点需要ack的数量
     */
    private void inputMsgToAckMap(ReplicationMsgEvent replicationMsgEvent, int needAckCount) {
        SlaveAckDTO slaveAckDTO = new SlaveAckDTO(new AtomicInteger(needAckCount), replicationMsgEvent.getChannelHandlerContext());
        CommonCache.getAckMap().put(replicationMsgEvent.getMsgId(), slaveAckDTO);
    }

    /**
     * 发送数据给到从节点
     *
     * @param replicationMsgEvent 主节点需要发送的数据
     */
    private void sendMsgToSlave(ReplicationMsgEvent replicationMsgEvent) {
        Map<String, ChannelHandlerContext> channelHandlerContextMap = CommonCache.getReplicationChannelManager().getValidSalveChannelMap();
        //判断当前采用的同步模式是哪种方式
        for (String reqId : channelHandlerContextMap.keySet()) {
            replicationMsgEvent.setChannelHandlerContext(null);
            byte[] body = JSON.toJSONBytes(replicationMsgEvent);
            //异步复制，直接发送给从节点，然后通知broker注册成功
            channelHandlerContextMap.get(reqId).writeAndFlush(new TcpMsg(NameServerEventCode.MASTER_REPLICATION_MSG.getCode(), body));
        }
    }
}
