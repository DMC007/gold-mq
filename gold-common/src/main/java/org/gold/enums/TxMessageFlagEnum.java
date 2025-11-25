package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/25
 * @description 事务消息枚举类型
 */
public enum TxMessageFlagEnum {
    HALF_MSG(0, "半提交消息"),
    REMAIN_HALF_ACK(1, "剩余半条ack消息");

    private int code;
    private String desc;

    TxMessageFlagEnum(int code, String desc) {
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
