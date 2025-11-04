package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public enum MessageSendWay {
    /**
     * 同步发送
     */
    SYNC(1),
    /**
     * 异步发送
     */
    ASYNC(2),
    ;
    private int code;

    MessageSendWay(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
