package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
public class SendMessageToBrokerResponseDTO extends BaseBrokerRemoteDTO {

    /**
     * 发送消息的结果状态
     *
     * @see org.gold.enums.SendMessageToBrokerResponseStatus
     */
    private int status;
    private String desc;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
