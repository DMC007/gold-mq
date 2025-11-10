package org.gold.core;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.common.BrokerServerSyncFutureManager;
import org.gold.constants.BrokerConstants;
import org.gold.dto.MessageDTO;
import org.gold.dto.SendMessageToBrokerResponseDTO;
import org.gold.dto.SlaveSyncRespDTO;
import org.gold.enums.*;
import org.gold.event.model.PushMsgEvent;
import org.gold.remote.BrokerServerSyncFuture;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author zhaoxun
 * @date 2025/10/24
 */
public class CommitLogAppendHandler {
    private static final Logger log = LogManager.getLogger(CommitLogAppendHandler.class);

    public void prepareMMapLoading(String topicName) throws IOException {
        CommitLogMMapFileModel commitLogMMapFileModel = new CommitLogMMapFileModel();
        //文件映射
        commitLogMMapFileModel.loadFileToMMap(topicName, 0, BrokerConstants.COMMIT_LOG_DEFAULT_MMAP_SIZE);
        CommonCache.getCommitLogMMapFileModelManager().put(topicName, commitLogMMapFileModel);
    }

    //TODO 消息追加V1
    public void appendMessage(MessageDTO messageDTO) throws IOException {
        CommitLogMMapFileModel commitLogMMapFileModel = CommonCache.getCommitLogMMapFileModelManager().get(messageDTO.getTopic());
        if (commitLogMMapFileModel == null) {
            throw new RuntimeException("topic is invalid!");
        }
        commitLogMMapFileModel.writeContent(messageDTO, true);
    }

    public void appendMessage(MessageDTO messageDTO, PushMsgEvent event) throws IOException {
        CommonCache.getCommitLogAppendHandler().appendMessage(messageDTO);
        int sendWay = messageDTO.getSendWay();
        boolean isAsyncSend = MessageSendWay.ASYNC.getCode() == sendWay;
        boolean isClusterMode = BrokerClusterModeEnum.MASTER_SLAVE.getCode().equals(CommonCache.getGlobalProperties().getBrokerClusterMode());
        boolean isDelayMsg = messageDTO.getDelay() > 0;
        boolean isMasterNode = "master".equals(CommonCache.getGlobalProperties().getBrokerClusterRole());
        if (isClusterMode) {
            //TODO 集群处理
            if (isMasterNode) {
                //先定义消息体，因为writeAndFlush是异步，若之后再定义，很可能同步很快对方响应过来，结果通过manager去获取需要的信息时为空，则response无法设置
                BrokerServerSyncFuture brokerServerSyncFuture = new BrokerServerSyncFuture();
                brokerServerSyncFuture.setMsgId(messageDTO.getMsgId());
                BrokerServerSyncFutureManager.putSyncFuture(messageDTO.getMsgId(), brokerServerSyncFuture);
                //主节点发送同步请求给从节点，异步发送是没有消息ID的
                for (ChannelHandlerContext slaveChannelCtx : CommonCache.getSlaveChannelMap().values()) {
                    slaveChannelCtx.writeAndFlush(new TcpMsg(BrokerEventCode.PUSH_MSG.getCode(), JSON.toJSONBytes(messageDTO)));
                }
                if (isAsyncSend || isDelayMsg) {
                    //这里移除，也表明主节点不需要响应，那么在处理响应的时候判空即可，这样可用保证manager里面不会有无用数据
                    BrokerServerSyncFutureManager.removeSyncFuture(messageDTO.getMsgId());
                    return;
                }
                //主从一开始是正常的，但后边从节点断开
                if (CommonCache.getSlaveChannelMap().isEmpty()) {
                    //这里移除，也表明主节点不需要响应，那么在处理响应的时候判空即可，这样可用保证manager里面不会有无用数据
                    BrokerServerSyncFutureManager.removeSyncFuture(messageDTO.getMsgId());
                    //可能从节点全部断开，无法同步，此时之间返回成功给客户端，保证整体可用
                    SendMessageToBrokerResponseDTO sendMessageToBrokerResponseDTO = new SendMessageToBrokerResponseDTO();
                    sendMessageToBrokerResponseDTO.setMsgId(messageDTO.getMsgId());
                    sendMessageToBrokerResponseDTO.setStatus(SendMessageToBrokerResponseStatus.SUCCESS.getCode());
                    sendMessageToBrokerResponseDTO.setDesc("send msg success, but current time has no slave node!!!");
                    TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.SEND_MSG_RESP.getCode(), JSON.toJSONBytes(sendMessageToBrokerResponseDTO));
                    event.getChannelHandlerContext().writeAndFlush(tcpMsg);
                    return;
                }
                BrokerServerSyncFuture slaveSyncAckRespFuture = BrokerServerSyncFutureManager.getSyncFuture(messageDTO.getMsgId());
                if (slaveSyncAckRespFuture != null) {
                    SlaveSyncRespDTO slaveSyncRespDTO = null;
                    SendMessageToBrokerResponseDTO sendMessageToBrokerResponseDTO = new SendMessageToBrokerResponseDTO();
                    sendMessageToBrokerResponseDTO.setMsgId(messageDTO.getMsgId());
                    sendMessageToBrokerResponseDTO.setStatus(SendMessageToBrokerResponseStatus.FAIL.getCode());
                    try {
                        slaveSyncRespDTO = (SlaveSyncRespDTO) slaveSyncAckRespFuture.get(3, TimeUnit.SECONDS);
                        if (slaveSyncRespDTO.isSyncSuccess()) {
                            sendMessageToBrokerResponseDTO.setStatus(SendMessageToBrokerResponseStatus.SUCCESS.getCode());
                        }
                        //超时等同步一系列问题全部注入到响应体中返回给到客户端
                    } catch (InterruptedException e) {
                        sendMessageToBrokerResponseDTO.setDesc("Slave node sync fail! Sync task had InterruptedException!");
                        log.error("slave sync error is:", e);
                    } catch (ExecutionException e) {
                        sendMessageToBrokerResponseDTO.setDesc("Slave node sync fail! Sync task had ExecutionException");
                        log.error("slave sync error is:", e);
                    } catch (TimeoutException e) {
                        sendMessageToBrokerResponseDTO.setDesc("Slave node sync fail! Sync task had TimeoutException");
                        log.error("slave sync error is:", e);
                    } catch (Exception e) {
                        sendMessageToBrokerResponseDTO.setDesc("Slave node sync unKnow error! Sync task had Exception");
                        log.error("slave sync unKnow error is:", e);
                    }
                    //响应返回给到客户端，完成主从复制链路效果
                    TcpMsg responseMsg = new TcpMsg(BrokerResponseCode.SEND_MSG_RESP.getCode(), JSON.toJSONBytes(sendMessageToBrokerResponseDTO));
                    event.getChannelHandlerContext().writeAndFlush(responseMsg);
                }
            } else {
                if (isAsyncSend || isDelayMsg) {
                    return;
                }
                //从节点响应主节点信息
                SlaveSyncRespDTO slaveSyncRespDTO = new SlaveSyncRespDTO();
                slaveSyncRespDTO.setMsgId(messageDTO.getMsgId());
                slaveSyncRespDTO.setSyncSuccess(true);
                TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.SLAVE_SYNC_RESP.getCode(), JSON.toJSONBytes(slaveSyncRespDTO));
                event.getChannelHandlerContext().writeAndFlush(tcpMsg);
                return;
            }
        } else {
            //异步或延迟消息之间返回
            if (isAsyncSend || isDelayMsg) {
                return;
            }
            //同步消息响应结果
            SendMessageToBrokerResponseDTO sendMessageToBrokerResponseDTO = new SendMessageToBrokerResponseDTO();
            sendMessageToBrokerResponseDTO.setMsgId(messageDTO.getMsgId());
            sendMessageToBrokerResponseDTO.setStatus(SendMessageToBrokerResponseStatus.SUCCESS.getCode());
            TcpMsg tcpMsg = new TcpMsg(BrokerResponseCode.SEND_MSG_RESP.getCode(), JSON.toJSONBytes(sendMessageToBrokerResponseDTO));
            event.getChannelHandlerContext().writeAndFlush(tcpMsg);
        }
    }
}
