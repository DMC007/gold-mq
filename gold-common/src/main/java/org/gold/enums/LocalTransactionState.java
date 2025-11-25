package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/25
 * @description 本地事务执行状态枚举
 */
public enum LocalTransactionState {
    COMMIT(0),
    ROLLBACK(1),
    UNKNOW(2),
    ;

    private int code;

    LocalTransactionState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static LocalTransactionState getByCode(int code) {
        for (LocalTransactionState state : LocalTransactionState.values()) {
            if (state.code == code) {
                return state;
            }
        }
        return null;
    }
}
