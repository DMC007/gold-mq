package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/18
 */
public enum AckStatus {
    SUCCESS(1),
    FAIL(0),
    ;
    private int code;

    AckStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
