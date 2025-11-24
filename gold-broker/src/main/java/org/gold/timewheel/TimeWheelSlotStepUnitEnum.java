package org.gold.timewheel;

/**
 * @author zhaoxun
 * @date 2025/11/24
 * @description 时间轮槽步进单位枚举
 */
public enum TimeWheelSlotStepUnitEnum {
    SECOND("second"),
    MINUTE("minute"),
    HOUR("hour"),
    ;

    private String code;

    TimeWheelSlotStepUnitEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
