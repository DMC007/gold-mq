package org.gold.timewheel;

/**
 * @author zhaoxun
 * @date 2025/11/24
 */
public class DelayMessageDTO {
    /**
     * 原始数据
     */
    private Object data;
    /**
     * 消息类型
     */
    private SlotStoreTypeEnum slotStoreType;
    /**
     * 延迟多久 秒
     */
    private int delay;

    private long nextExecuteTime;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public SlotStoreTypeEnum getSlotStoreType() {
        return slotStoreType;
    }

    public void setSlotStoreType(SlotStoreTypeEnum slotStoreType) {
        this.slotStoreType = slotStoreType;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public long getNextExecuteTime() {
        return nextExecuteTime;
    }

    public void setNextExecuteTime(long nextExecuteTime) {
        this.nextExecuteTime = nextExecuteTime;
    }
}
