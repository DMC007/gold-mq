package org.gold.model;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public class GoldMqTopicModel {
    private String topic;
    private Long createAt;
    private Long updateAt;
    private CommitLogModel commitLogModel;
    private List<QueueModel> queueList;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Long createAt) {
        this.createAt = createAt;
    }

    public Long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Long updateAt) {
        this.updateAt = updateAt;
    }

    public CommitLogModel getCommitLogModel() {
        return commitLogModel;
    }

    public void setCommitLogModel(CommitLogModel commitLogModel) {
        this.commitLogModel = commitLogModel;
    }

    public List<QueueModel> getQueueList() {
        return queueList;
    }

    public void setQueueList(List<QueueModel> queueList) {
        this.queueList = queueList;
    }

    @Override
    public String toString() {
        return "GoldMqTopicModel{" +
                "topic='" + topic + '\'' +
                ", createAt=" + createAt +
                ", updateAt=" + updateAt +
                ", commitLogModel=" + commitLogModel +
                ", queueList=" + queueList +
                '}';
    }
}
