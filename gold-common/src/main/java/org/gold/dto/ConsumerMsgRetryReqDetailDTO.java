package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/19
 */
public class ConsumerMsgRetryReqDetailDTO {
    private String topic;
    private String consumerGroup;
    private Integer queueId;
    private String ip;
    private Integer port;
    private Long commitLogOffset;
    private Integer commitLogMsgLength;
    private String commitLogName;
    //重试次数
    private int retryTime;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public Integer getQueueId() {
        return queueId;
    }

    public void setQueueId(Integer queueId) {
        this.queueId = queueId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getCommitLogOffset() {
        return commitLogOffset;
    }

    public void setCommitLogOffset(Long commitLogOffset) {
        this.commitLogOffset = commitLogOffset;
    }

    public Integer getCommitLogMsgLength() {
        return commitLogMsgLength;
    }

    public void setCommitLogMsgLength(Integer commitLogMsgLength) {
        this.commitLogMsgLength = commitLogMsgLength;
    }

    public String getCommitLogName() {
        return commitLogName;
    }

    public void setCommitLogName(String commitLogName) {
        this.commitLogName = commitLogName;
    }

    public int getRetryTime() {
        return retryTime;
    }

    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }
}
