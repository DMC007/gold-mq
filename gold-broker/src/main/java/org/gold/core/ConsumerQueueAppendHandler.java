package org.gold.core;

import org.gold.cache.CommonCache;
import org.gold.model.GoldMqTopicModel;
import org.gold.model.QueueModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/10/24
 */
public class ConsumerQueueAppendHandler {

    public void prepareConsumerQueue(String topicName) throws IOException {
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topicName);
        List<QueueModel> queueList = goldMqTopicModel.getQueueList();
        if (queueList == null) {
            return;
        }
        List<ConsumerQueueMMapFileModel> consumerQueueMMapFileModelList = new ArrayList<>();
        for (QueueModel queueModel : queueList) {
            ConsumerQueueMMapFileModel consumerQueueMMapFileModel = new ConsumerQueueMMapFileModel();
            consumerQueueMMapFileModel.loadFileToMMap(
                    topicName,
                    queueModel.getId(),
                    queueModel.getLastOffset(),
                    queueModel.getLatestOffset().get(),
                    queueModel.getOffsetLimit()
            );
            consumerQueueMMapFileModelList.add(consumerQueueMMapFileModel);
        }
        CommonCache.getConsumerQueueMMapFileModelManager().put(topicName, consumerQueueMMapFileModelList);
    }
}
