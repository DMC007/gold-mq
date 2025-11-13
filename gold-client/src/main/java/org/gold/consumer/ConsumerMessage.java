package org.gold.consumer;

import org.gold.dto.ConsumerMsgCommitLogDTO;

/**
 * @author zhaoxun
 * @date 2025/11/12
 */
public class ConsumerMessage {
    private int queueId;
    private ConsumerMsgCommitLogDTO consumerMsgCommitLogDTO;

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public ConsumerMsgCommitLogDTO getConsumerMsgCommitLogDTO() {
        return consumerMsgCommitLogDTO;
    }

    public void setConsumerMsgCommitLogDTO(ConsumerMsgCommitLogDTO consumerMsgCommitLogDTO) {
        this.consumerMsgCommitLogDTO = consumerMsgCommitLogDTO;
    }
}
