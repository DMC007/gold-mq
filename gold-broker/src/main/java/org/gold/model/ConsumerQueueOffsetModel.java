package org.gold.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description 对topic和消费组对应消费偏移量文件的映射, 参考consumerqueue-offset.json文件格式
 */
public class ConsumerQueueOffsetModel {

    private OffsetTable offsetTable = new OffsetTable();

    public static class OffsetTable {
        private Map<String, ConsumerGroupDetail> topicConsumerGroupDetail = new HashMap<>();

        public Map<String, ConsumerGroupDetail> getTopicConsumerGroupDetail() {
            return topicConsumerGroupDetail;
        }

        public void setTopicConsumerGroupDetail(Map<String, ConsumerGroupDetail> topicConsumerGroupDetail) {
            this.topicConsumerGroupDetail = topicConsumerGroupDetail;
        }
    }

    public static class ConsumerGroupDetail {
        private Map<String, Map<String, String>> consumerGroupDetailMap = new HashMap<>();

        public Map<String, Map<String, String>> getConsumerGroupDetailMap() {
            return consumerGroupDetailMap;
        }

        public void setConsumerGroupDetailMap(Map<String, Map<String, String>> consumerGroupDetailMap) {
            this.consumerGroupDetailMap = consumerGroupDetailMap;
        }
    }

    public OffsetTable getOffsetTable() {
        return offsetTable;
    }

    public void setOffsetTable(OffsetTable offsetTable) {
        this.offsetTable = offsetTable;
    }
}
