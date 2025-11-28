package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/27
 */
public enum ReplicationModeEnum {
    MASTER_SLAVE("master_slave", "主从复制模式"),
    TRACE("trace", "链路复制模式");
    private String code;
    private String desc;

    ReplicationModeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ReplicationModeEnum getByCode(String code) {
        for (ReplicationModeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
