package org.gold.event.model;

import org.gold.timewheel.TimeWheelSlotModel;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/24
 */
public class TimeWheelEvent extends Event {
    private List<TimeWheelSlotModel> timeWheelSlotModelList;

    public List<TimeWheelSlotModel> getTimeWheelSlotModelList() {
        return timeWheelSlotModelList;
    }

    public void setTimeWheelSlotModelList(List<TimeWheelSlotModel> timeWheelSlotModelList) {
        this.timeWheelSlotModelList = timeWheelSlotModelList;
    }
}
