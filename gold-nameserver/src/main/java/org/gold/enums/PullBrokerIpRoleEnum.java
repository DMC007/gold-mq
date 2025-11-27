package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/27
 */
public enum PullBrokerIpRoleEnum {
    MASTER("master"),
    SLAVE("slave"),
    SINGLE("single"),
    ;
    private String code;

    PullBrokerIpRoleEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
