package org.gold.timewheel;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/24
 * @description 时间轮槽列表模型, 这里是代表每一个slot下面，对应的一些列需要执行的任务
 */
public class TimeWheelSlotListModel {
    private List<TimeWheelSlotModel> timeWheelSlotModelList = new LinkedList<>();

    public List<TimeWheelSlotModel> getTimeWheelSlotModelList() {
        return timeWheelSlotModelList;
    }

    public void setTimeWheelSlotModelList(List<TimeWheelSlotModel> timeWheelSlotModelList) {
        this.timeWheelSlotModelList = timeWheelSlotModelList;
    }
}
