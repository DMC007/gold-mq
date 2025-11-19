package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/18
 */
public class ConsumerMsgAckRespDTO extends BaseBrokerRemoteDTO {

    /**
     * ack响应是否成功
     *
     * @see org.gold.enums.AckStatus
     */
    private int ackStatus;

    public int getAckStatus() {
        return ackStatus;
    }

    public void setAckStatus(int ackStatus) {
        this.ackStatus = ackStatus;
    }
}
