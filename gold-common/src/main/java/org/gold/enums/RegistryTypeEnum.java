package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public enum RegistryTypeEnum {
    PRODUCER("producer"),
    CONSUMER("consumer"),
    BROKER("broker");
    private String code;

    RegistryTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
