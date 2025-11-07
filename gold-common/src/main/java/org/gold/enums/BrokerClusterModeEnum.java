package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public enum BrokerClusterModeEnum {
    MASTER_SLAVE("master-slave"),
    ;
    private String code;

    BrokerClusterModeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
