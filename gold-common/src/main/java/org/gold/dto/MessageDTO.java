package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description mq消息发送参数
 */
public class MessageDTO {
    private String topic;
    /**
     * 消息是否有指定发往哪个topic队列
     */
    private int queueId = -1;
    private String msgId;
    /**
     * 发送方式（同步/异步）
     *
     * @see org.gold.enums.MessageSendWay
     */
    private int sendWay;
    private byte[] body;
    private boolean isRetry;
    private int currentRetryTimes;
    //延迟的时间 秒单位
    private int delay;
    private String producerId;
    //TODO 缺少事务消息相关的属性, 后期加上

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getSendWay() {
        return sendWay;
    }

    public void setSendWay(int sendWay) {
        this.sendWay = sendWay;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public boolean isRetry() {
        return isRetry;
    }

    public void setRetry(boolean retry) {
        isRetry = retry;
    }

    public int getCurrentRetryTimes() {
        return currentRetryTimes;
    }

    public void setCurrentRetryTimes(int currentRetryTimes) {
        this.currentRetryTimes = currentRetryTimes;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }
}
