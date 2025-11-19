package org.gold.dto;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/18
 */
public class ConsumerMsgRespDTO {
    /**
     * 队列id
     */
    private Integer queueId;
    /**
     * 拉数据返回内容
     */
    private List<ConsumerMsgCommitLogDTO> commitLogContentList;

    public Integer getQueueId() {
        return queueId;
    }

    public void setQueueId(Integer queueId) {
        this.queueId = queueId;
    }

    public List<ConsumerMsgCommitLogDTO> getCommitLogContentList() {
        return commitLogContentList;
    }

    public void setCommitLogContentList(List<ConsumerMsgCommitLogDTO> commitLogContentList) {
        this.commitLogContentList = commitLogContentList;
    }
}
