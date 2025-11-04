package org.gold.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/10/22
 * @description consumerqueue的mmap映射对象的管理器
 */
public class ConsumerQueueMMapFileModelManager {

    private Map<String, List<ConsumerQueueMMapFileModel>> consumerQueueMMapFileModelMap = new HashMap<>();

    public void put(String topic, List<ConsumerQueueMMapFileModel> consumeQueueMMapFileModelList) {
        consumerQueueMMapFileModelMap.put(topic, consumeQueueMMapFileModelList);
    }

    public List<ConsumerQueueMMapFileModel> get(String topic) {
        return consumerQueueMMapFileModelMap.get(topic);
    }
}
