package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/12
 */
public class CreateTopicReqDTO extends BaseBrokerRemoteDTO {
    private String topic;
    private Integer queueSize;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }
}
