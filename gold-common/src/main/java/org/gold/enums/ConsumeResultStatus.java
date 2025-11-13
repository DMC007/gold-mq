package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/12
 */
public enum ConsumeResultStatus {
    CONSUME_SUCCESS(1),
    CONSUME_LATER(2),
    ;

    private int code;

    ConsumeResultStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
