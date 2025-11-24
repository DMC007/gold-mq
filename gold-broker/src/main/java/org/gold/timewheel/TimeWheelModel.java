package org.gold.timewheel;

import org.gold.utils.AssertUtils;

/**
 * @author zhaoxun
 * @date 2025/11/24
 */
public class TimeWheelModel {
    private int current;
    private TimeWheelSlotListModel[] timeWheelSlotListModels;

    /**
     * 时间轮的存储时间单位【时间轮可以是秒轮，分钟轮，天轮】
     *
     * @see TimeWheelSlotStepUnitEnum
     */
    private String unit;

    public int countNextSlot(int delay) {
        AssertUtils.isTrue(delay < timeWheelSlotListModels.length, "delay can not large than slot's total count");
        int remainSlotCount = timeWheelSlotListModels.length - current;
        int diff = delay - remainSlotCount;
        if (diff < 0) {
            return current + delay;
        }
        return diff;
    }


    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public TimeWheelSlotListModel[] getTimeWheelSlotListModels() {
        return timeWheelSlotListModels;
    }

    public void setTimeWheelSlotListModels(TimeWheelSlotListModel[] timeWheelSlotListModels) {
        this.timeWheelSlotListModels = timeWheelSlotListModels;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
