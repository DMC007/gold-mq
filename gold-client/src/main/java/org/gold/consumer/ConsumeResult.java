package org.gold.consumer;

import org.gold.enums.ConsumeResultStatus;

/**
 * @author zhaoxun
 * @date 2025/11/12
 */
public class ConsumeResult {
    /**
     * 消费结果
     */
    private int consumeResultStatus;

    public ConsumeResult(int consumeResultStatus) {
        this.consumeResultStatus = consumeResultStatus;
    }

    public int getConsumeResultStatus() {
        return consumeResultStatus;
    }

    public void setConsumeResultStatus(int consumeResultStatus) {
        this.consumeResultStatus = consumeResultStatus;
    }

    public static ConsumeResult CONSUME_SUCCESS() {
        return new ConsumeResult(ConsumeResultStatus.CONSUME_SUCCESS.getCode());
    }

    public static ConsumeResult CONSUME_LATER() {
        return new ConsumeResult(ConsumeResultStatus.CONSUME_LATER.getCode());
    }
}
