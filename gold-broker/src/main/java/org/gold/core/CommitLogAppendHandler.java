package org.gold.core;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.constants.BrokerConstants;
import org.gold.dto.MessageDTO;
import org.gold.dto.SendMessageToBrokerResponseDTO;
import org.gold.enums.BrokerClusterModeEnum;
import org.gold.enums.BrokerResponseCode;
import org.gold.enums.MessageSendWay;
import org.gold.enums.SendMessageToBrokerResponseStatus;
import org.gold.event.model.PushMsgEvent;

import java.io.IOException;

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
        if (isClusterMode) {
            //TODO 集群处理
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
