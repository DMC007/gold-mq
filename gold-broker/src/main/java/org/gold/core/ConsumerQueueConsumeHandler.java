package org.gold.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;
import org.gold.dto.ConsumerMsgCommitLogDTO;
import org.gold.model.*;
import org.gold.utils.LogFileNameUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/10/24
 * @description 消费队列消费处理器
 */
public class ConsumerQueueConsumeHandler {

    private static final Logger log = LogManager.getLogger(ConsumerQueueConsumeHandler.class);

    /**
     * 读取当前最新N条consumerQueue的消息内容,并且返回commitLog原始数据
     *
     * @param consumerQueueConsumeReqModel 消费请求参数
     * @return 消费结果
     */
    public List<ConsumerMsgCommitLogDTO> consume(ConsumerQueueConsumeReqModel consumerQueueConsumeReqModel) {
        String topic = consumerQueueConsumeReqModel.getTopic();
        //1. 校验参数合法性
        //2. 获取当前匹配的队列的最新的consumerQueue的offset是多少
        //3. 获取当前匹配队列存储文件的mmap对象，然后读取offset地址的数据
        GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
        if (goldMqTopicModel == null) {
            throw new RuntimeException("topic " + topic + " not exist");
        }
        String consumerGroup = consumerQueueConsumeReqModel.getConsumerGroup();
        Integer queueId = consumerQueueConsumeReqModel.getQueueId();
        Integer batchSize = consumerQueueConsumeReqModel.getBatchSize();

        ConsumerQueueOffsetModel.OffsetTable offsetTable = CommonCache.getConsumerQueueOffsetModel().getOffsetTable();
        Map<String, ConsumerQueueOffsetModel.ConsumerGroupDetail> topicConsumerGroupDetail = offsetTable.getTopicConsumerGroupDetail();
        ConsumerQueueOffsetModel.ConsumerGroupDetail consumerGroupDetail = topicConsumerGroupDetail.get(topic);
        //如果是首次消费
        if (consumerGroupDetail == null) {
            consumerGroupDetail = new ConsumerQueueOffsetModel.ConsumerGroupDetail();
            topicConsumerGroupDetail.put(topic, consumerGroupDetail);
        }
        Map<String, Map<String, String>> consumerGroupDetailMap = consumerGroupDetail.getConsumerGroupDetailMap();
        Map<String, String> queueIdOffsetMap = consumerGroupDetailMap.get(consumerGroup);
        List<QueueModel> queueList = goldMqTopicModel.getQueueList();
        //不存在消费offset就初始化
        if (queueIdOffsetMap == null) {
            queueIdOffsetMap = new HashMap<>();
            for (QueueModel queueModel : queueList) {
                queueIdOffsetMap.put(String.valueOf(queueModel.getId()), "00000000#0");
            }
            consumerGroupDetailMap.put(consumerGroup, queueIdOffsetMap);
        }
        //到这里queueIdOffsetMap是一定有值的
        String offsetStrInfo = queueIdOffsetMap.get(String.valueOf(queueId));
        String[] offsetStrArr = offsetStrInfo.split("#");
        int consumerQueueOffset = Integer.parseInt(offsetStrArr[1]);
        QueueModel queueModel = queueList.get(queueId);
        //消费到了尽头
        if (queueModel.getLatestOffset().get() <= consumerQueueOffset) {
            return null;
        }
        List<ConsumerQueueMMapFileModel> consumerQueueMMapFileModels = CommonCache.getConsumerQueueMMapFileModelManager().get(topic);
        ConsumerQueueMMapFileModel consumerQueueMMapFileModel = consumerQueueMMapFileModels.get(queueId);
        //一次读取多条consumerQueue的数据内容
        List<byte[]> consumerQueueContentList = consumerQueueMMapFileModel.readContent(consumerQueueOffset, batchSize);
        List<ConsumerMsgCommitLogDTO> commitLogBodyContentList = new ArrayList<>();
        for (byte[] content : consumerQueueContentList) {
            //跟进consumerQueue的内容确定commitLog的读取位置
            ConsumerQueueDetailModel consumerQueueDetailModel = new ConsumerQueueDetailModel();
            //构建consumerQueueDetailModel[数据转换]
            consumerQueueDetailModel.buildFromBytes(content);
            CommitLogMMapFileModel commitLogMMapFileModel = CommonCache.getCommitLogMMapFileModelManager().get(topic);
            ConsumerMsgCommitLogDTO commitLogContent = commitLogMMapFileModel.readContent(consumerQueueDetailModel.getMsgIndex(), consumerQueueDetailModel.getMsgLength());
            //设置重试次数
            commitLogContent.setRetryTimes(consumerQueueDetailModel.getRetryTimes());
            commitLogBodyContentList.add(commitLogContent);
        }
        return commitLogBodyContentList;
    }

    /**
     * 更新consumerQueue-offset的值
     *
     * @param topic         主题
     * @param consumerGroup 消费组
     * @param queueId       队列id
     * @return 是否成功
     */
    public boolean ack(String topic, String consumerGroup, int queueId) {
        try {
            ConsumerQueueOffsetModel.OffsetTable offsetTable = CommonCache.getConsumerQueueOffsetModel().getOffsetTable();
            Map<String, ConsumerQueueOffsetModel.ConsumerGroupDetail> topicConsumerGroupDetail = offsetTable.getTopicConsumerGroupDetail();
            ConsumerQueueOffsetModel.ConsumerGroupDetail consumerGroupDetail = topicConsumerGroupDetail.get(topic);
            Map<String, String> consumerQueueOffsetDetailMap = consumerGroupDetail.getConsumerGroupDetailMap().get(consumerGroup);
            String offsetStrInfo = consumerQueueOffsetDetailMap.get(String.valueOf(queueId));
            String[] offsetStrArr = offsetStrInfo.split("#");
            String fileName = offsetStrArr[0];
            int currentOffset = Integer.parseInt(offsetStrArr[1]);
            // 增加偏移量 (每个ConsumeQueue条目固定16字节)
            currentOffset += 16;

            // 检查是否需要切换到新文件
            // 参考BrokerConstants中定义的ConsumeQueue文件大小
            int maxConsumeQueueFileSize = BrokerConstants.COMSUMERQUEUE_DEFAULT_MMAP_SIZE; // 假设这是文件大小
            //如果等于限制值，下次消费ack应该从新文件开始消费
            if (currentOffset >= maxConsumeQueueFileSize) {
                // 需要切换到下一个ConsumerQueue文件
                String newFileName = LogFileNameUtil.incrConsumeQueueFileName(fileName);
                // 获取当前队列实际正在使用的文件名
                GoldMqTopicModel goldMqTopicModel = CommonCache.getGoldMqTopicModelMap().get(topic);
                List<QueueModel> queueList = goldMqTopicModel.getQueueList();
                QueueModel queueModel = queueList.get(queueId);
                String actualCurrentFileName = queueModel.getFileName();

                // 如果实际文件已经切换到新文件，则更新消费位点的文件名
                if (Long.parseLong(actualCurrentFileName) > Long.parseLong(fileName)) {
                    fileName = newFileName;
                    //说明原文件的消息是写满的，这里消费ack的时候说明不会浪费空间，既然消费完毕，切换到新文件直接定位到0
                    if (currentOffset == maxConsumeQueueFileSize) {
                        //切换到新文件时偏移量重置为0
                        currentOffset = 0;
                    } else {
                        //切换到新文件，该次消费是16字节
                        currentOffset = 16;
                    }
                } else if (Long.parseLong(actualCurrentFileName) == Long.parseLong(fileName)) {
                    // 文件名相同, 说明没新文件，那边ack应该抛异常
                    throw new RuntimeException("[topic:" + topic + ", consumerGroup:" + consumerGroup + ", queueId:" + queueId + "] ack error: fileName is same, please check your code");
                }
                // 如果实际文件还没切换，说明还没到切换时机，保持当前文件名和偏移量
            }
            consumerQueueOffsetDetailMap.put(String.valueOf(queueId), fileName + "#" + currentOffset);
            return true;
        } catch (Exception e) {
            log.error("[topic:{}, consumerGroup:{}, queueId:{}] ack error:{}", topic, consumerGroup, queueId, e.getMessage(), e);
            return false;
        }
    }
}
