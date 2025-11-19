package org.gold.rebalance;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zhaoxun
 * @date 2025/11/19
 * @description 消费者实例
 */
public class ConsumerInstance {

    private String ip;
    private Integer port;
    private String consumeGroup;
    private String topic;
    private Integer batchSize;
    private Set<Integer> queueIdSet = new HashSet<>();
    private String consumerReqId;

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

    public String getConsumeGroup() {
        return consumeGroup;
    }

    public void setConsumeGroup(String consumeGroup) {
        this.consumeGroup = consumeGroup;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Set<Integer> getQueueIdSet() {
        return queueIdSet;
    }

    public void setQueueIdSet(Set<Integer> queueIdSet) {
        this.queueIdSet = queueIdSet;
    }

    public String getConsumerReqId() {
        return consumerReqId;
    }

    public void setConsumerReqId(String consumerReqId) {
        this.consumerReqId = consumerReqId;
    }
}
