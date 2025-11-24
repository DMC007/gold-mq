package org.gold;

import org.gold.event.EventBus;
import org.gold.timewheel.DelayMessageDTO;
import org.gold.timewheel.SlotStoreTypeEnum;
import org.gold.timewheel.TimeWheelModelManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author zhaoxun
 * @date 2025/11/24
 */
public class TimeWheelTest {
    @Test
    public void test() {
        TimeWheelModelManager timeWheelModelManager = new TimeWheelModelManager();
        timeWheelModelManager.init(new EventBus("time-wheel-task"));
        DelayMessageDTO delayMessageDTO = new DelayMessageDTO();
        delayMessageDTO.setData("hello world");
        delayMessageDTO.setDelay(62);
        delayMessageDTO.setSlotStoreType(SlotStoreTypeEnum.MESSAGE_RETRY_DTO);
        timeWheelModelManager.add(delayMessageDTO);
        timeWheelModelManager.doScanTask();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
