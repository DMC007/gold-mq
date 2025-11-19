package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/19
 */
public class ConsumeMsgRetryRespDTO extends BaseBrokerRemoteDTO {

    /**
     * 1-成功 0-失败
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
