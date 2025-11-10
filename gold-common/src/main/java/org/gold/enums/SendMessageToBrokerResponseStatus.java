package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/10
 */
public enum SendMessageToBrokerResponseStatus {
    SUCCESS(0, "发送成功"),
    FAIL(1, "发送失败"),
    ;

    private int code;
    private String desc;

    SendMessageToBrokerResponseStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
