package org.gold.timewheel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.event.EventBus;
import org.gold.event.model.TimeWheelEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/11/24
 * @description 所有时间轮操作管理类, 时间轮处理消息类型：延迟消息，重试消息，事务消息
 */
public class TimeWheelModelManager {
    private static final Logger log = LogManager.getLogger(TimeWheelModelManager.class);

    private final Object secondsLock = new Object();
    private final Object minutesLock = new Object();

    private long executeSeconds = 0L;

    private TimeWheelModel secondsTimeWheelModel;
    private TimeWheelModel minutesTimeWheelModel;

    private EventBus eventBus;

    /**
     * 初始化时间轮内部的变量值
     *
     * @param eventBus 事件总线
     */
    public void init(EventBus eventBus) {
        secondsTimeWheelModel = new TimeWheelModel();
        secondsTimeWheelModel.setUnit(TimeWheelSlotStepUnitEnum.SECOND.getCode());
        secondsTimeWheelModel.setTimeWheelSlotListModels(buildTimeWheelSlotListModel(60));
        secondsTimeWheelModel.setCurrent(0);

        minutesTimeWheelModel = new TimeWheelModel();
        minutesTimeWheelModel.setUnit(TimeWheelSlotStepUnitEnum.MINUTE.getCode());
        minutesTimeWheelModel.setTimeWheelSlotListModels(buildTimeWheelSlotListModel(60));
        minutesTimeWheelModel.setCurrent(0);

        this.eventBus = eventBus;
        this.eventBus.init();
    }

    private TimeWheelSlotListModel[] buildTimeWheelSlotListModel(int count) {
        TimeWheelSlotListModel[] timeWheelSlotListModels = new TimeWheelSlotListModel[count];
        for (int i = 0; i < count; i++) {
            timeWheelSlotListModels[i] = new TimeWheelSlotListModel();
        }
        return timeWheelSlotListModels;
    }

    public void add(DelayMessageDTO delayMessageDTO) {
        int delay = delayMessageDTO.getDelay();
        int min = delay / 60;
        if (min == 0) {
            synchronized (secondsLock) {
                int nextSlot = secondsTimeWheelModel.countNextSlot(delay);
                log.info("current second slot:{}, next slot:{}", secondsTimeWheelModel.getCurrent(), nextSlot);
                TimeWheelSlotListModel timeWheelSlotListModel = secondsTimeWheelModel.getTimeWheelSlotListModels()[nextSlot];
                //每个槽位下面的列表里面的每个任务
                TimeWheelSlotModel timeWheelSlotModel = new TimeWheelSlotModel();
                timeWheelSlotModel.setData(delayMessageDTO.getData());
                timeWheelSlotModel.setDelaySeconds(delayMessageDTO.getDelay());
                timeWheelSlotModel.setNextExecuteTime(delayMessageDTO.getNextExecuteTime());
                timeWheelSlotModel.setStoreType(delayMessageDTO.getSlotStoreType().getClazz());
                //添加到时间轮slot的列表中
                timeWheelSlotListModel.getTimeWheelSlotModelList().add(timeWheelSlotModel);
            }
        } else if (min > 0) {
            synchronized (minutesLock) {
                int nextSlot = minutesTimeWheelModel.countNextSlot(min);
                log.info("current minute slot:{}, next slot:{}", minutesTimeWheelModel.getCurrent(), nextSlot);
                TimeWheelSlotListModel timeWheelSlotListModel = minutesTimeWheelModel.getTimeWheelSlotListModels()[nextSlot];
                //每个槽位下面的列表里面的每个任务
                TimeWheelSlotModel timeWheelSlotModel = new TimeWheelSlotModel();
                timeWheelSlotModel.setData(delayMessageDTO.getData());
                timeWheelSlotModel.setDelaySeconds(delayMessageDTO.getDelay());
                timeWheelSlotModel.setNextExecuteTime(delayMessageDTO.getNextExecuteTime());
                timeWheelSlotModel.setStoreType(delayMessageDTO.getSlotStoreType().getClazz());
                //添加到时间轮slot的列表中
                timeWheelSlotListModel.getTimeWheelSlotModelList().add(timeWheelSlotModel);
            }
        }
    }

    /**
     * 开启时间轮扫描slot数组的任务
     */
    public void doScanTask() {
        new Thread(() -> {
            log.info("start scan slot task");
            while (true) {
                try {
                    //秒轮执行
                    doSecondsTimeWheelExecute();
                    if (executeSeconds % 60 == 0) {
                        //分钟轮执行
                        doMinutesTimeWheelExecute();
                    }
                    TimeUnit.SECONDS.sleep(1);
                    executeSeconds++;
                } catch (Exception e) {
                    log.error("scan slot task error", e);
                }
            }
        }, "scan-slot-task").start();
    }

    private void doMinutesTimeWheelExecute() {
        synchronized (minutesLock) {
            log.info("do scan minutes slots:{}", minutesTimeWheelModel.getCurrent());
            int current = minutesTimeWheelModel.getCurrent();
            TimeWheelSlotListModel timeWheelSlotListModel = minutesTimeWheelModel.getTimeWheelSlotListModels()[current];
            List<TimeWheelSlotModel> timeWheelSlotModelList = timeWheelSlotListModel.getTimeWheelSlotModelList();
            //当前需要被执行的列表
            List<TimeWheelSlotModel> minutesTimeWheelModelList = new LinkedList<>();
            for (TimeWheelSlotModel timeWheelSlotModel : timeWheelSlotModelList) {
                int remainSecond = timeWheelSlotModel.getDelaySeconds() % 60;
                //如果延迟的时间不是分钟的整数
                if (remainSecond > 0) {
                    //需要被扔回到秒级别的时间轮
                    DelayMessageDTO delayMessageDTO = new DelayMessageDTO();
                    delayMessageDTO.setData(timeWheelSlotModel.getData());
                    delayMessageDTO.setDelay(remainSecond);
                    delayMessageDTO.setSlotStoreType(SlotStoreTypeEnum.MESSAGE_RETRY_DTO);
                    delayMessageDTO.setNextExecuteTime(timeWheelSlotModel.getNextExecuteTime());
                    add(delayMessageDTO);
                    log.info("add message to second timeWheel");
                } else {
                    minutesTimeWheelModelList.add(timeWheelSlotModel);
                }
            }
            if (CollectionUtils.isNotEmpty(minutesTimeWheelModelList)) {
                log.info("minutes msg:{}", minutesTimeWheelModelList.getFirst().getData());
                //分钟整 直接执行业务逻辑
                TimeWheelEvent timeWheelEvent = new TimeWheelEvent();
                timeWheelEvent.setTimeWheelSlotModelList(minutesTimeWheelModelList);
                eventBus.publish(timeWheelEvent);
            }
            //执行完成的任务，需要清理掉
            timeWheelSlotListModel.setTimeWheelSlotModelList(new LinkedList<>());
            if (current == minutesTimeWheelModel.getTimeWheelSlotListModels().length - 1) {
                current = 0;
            } else {
                current = current + 1;
            }
            minutesTimeWheelModel.setCurrent(current);
        }
    }

    private void doSecondsTimeWheelExecute() {
        synchronized (secondsLock) {
            int current = secondsTimeWheelModel.getCurrent();
            TimeWheelSlotListModel timeWheelSlotListModel = secondsTimeWheelModel.getTimeWheelSlotListModels()[current];
            List<TimeWheelSlotModel> timeWheelSlotModelList = timeWheelSlotListModel.getTimeWheelSlotModelList();
            if (CollectionUtils.isNotEmpty(timeWheelSlotModelList)) {
                log.info("seconds msg:{}", timeWheelSlotModelList.getFirst().getData());
                TimeWheelEvent timeWheelEvent = new TimeWheelEvent();
                timeWheelEvent.setTimeWheelSlotModelList(timeWheelSlotModelList);
                eventBus.publish(timeWheelEvent);
            }
            //执行完任务，清空槽位数据[上面的执行是异步执行，出异常由业务处理，时间轮不支持异常执行处理]
            timeWheelSlotListModel.setTimeWheelSlotModelList(new LinkedList<>());
            if (current == secondsTimeWheelModel.getTimeWheelSlotListModels().length - 1) {
                current = 0;
            } else {
                current = current + 1;
            }
            secondsTimeWheelModel.setCurrent(current);
        }
    }
}
