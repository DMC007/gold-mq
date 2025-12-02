package org.gold.rebalance.strategy;

import org.gold.rebalance.ConsumerInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhaoxun
 * @date 2025/12/2
 * @description 需要参与重平衡的消费组
 */
public class ReBalanceInfo {
    private Map<String, List<ConsumerInstance>> consumeInstanceMap;
    //消费者发生变化的消费组 key：topic; value：消费组名称
    private Map<String, Set<String>> changeConsumerGroupMap = new HashMap<>();

    public Map<String, List<ConsumerInstance>> getConsumeInstanceMap() {
        return consumeInstanceMap;
    }

    public void setConsumeInstanceMap(Map<String, List<ConsumerInstance>> consumeInstanceMap) {
        this.consumeInstanceMap = consumeInstanceMap;
    }

    public Map<String, Set<String>> getChangeConsumerGroupMap() {
        return changeConsumerGroupMap;
    }

    public void setChangeConsumerGroupMap(Map<String, Set<String>> changeConsumerGroupMap) {
        this.changeConsumerGroupMap = changeConsumerGroupMap;
    }
}
