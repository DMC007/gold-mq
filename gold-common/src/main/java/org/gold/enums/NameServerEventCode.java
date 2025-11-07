package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public enum NameServerEventCode {
    REGISTRY(1, "注册事件"),
    UN_REGISTRY(2, "下线事件"),
    HEART_BEAT(3, "心跳事件"),
    PULL_BROKER_IP_LIST(11, "拉取broker的主节点ip地址"),
    ;
    private Integer code;
    private String desc;

    NameServerEventCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
