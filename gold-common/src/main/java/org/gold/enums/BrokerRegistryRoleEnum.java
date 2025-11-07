package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public enum BrokerRegistryRoleEnum {

    MASTER("master"),
    SLAVE("slave"),
    SINGLE("single"),
    ;

    String code;

    BrokerRegistryRoleEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static BrokerRegistryRoleEnum getByCode(String code) {
        for (BrokerRegistryRoleEnum brokerRegistryRoleEnum : BrokerRegistryRoleEnum.values()) {
            if (brokerRegistryRoleEnum.code.equals(code)) {
                return brokerRegistryRoleEnum;
            }
        }
        return null;
    }
}
