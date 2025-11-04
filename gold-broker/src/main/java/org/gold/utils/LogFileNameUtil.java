package org.gold.utils;

import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public class LogFileNameUtil {

    /**
     * 构建第一份commitLog文件名称
     *
     * @return commitLog文件名称
     */
    public static String buildFirstCommitLogName() {
        return "00000000";
    }

    /**
     * 构建第一份consumeQueue文件名称
     *
     * @return consumeQueue文件名称
     */
    public static String buildFirstConsumeQueueName() {
        return "00000000";
    }

    /**
     * 构建commitLog文件路径
     *
     * @param topicName         消息主题
     * @param commitLogFileName commitLog文件名称
     * @return commitLog文件路径
     */
    public static String buildCommitLogFilePath(String topicName, String commitLogFileName) {
        return CommonCache.getGlobalProperties().getGoldMqHome()
                + BrokerConstants.BASE_COMMIT_PATH
                + topicName
                + BrokerConstants.SPLIT
                + commitLogFileName;
    }

    /**
     * 构建commitLog所在目录的基本路径
     *
     * @param topicName 消息主题
     * @return commitLog所在目录的基本路径
     */
    public static String buildCommitLogBasePath(String topicName) {
        return CommonCache.getGlobalProperties().getGoldMqHome()
                + BrokerConstants.BASE_COMMIT_PATH
                + topicName;
    }

    /**
     * 构建consumeQueue的文件路径
     *
     * @param topicName            消息主题
     * @param queueId              消费队列Id
     * @param consumeQueueFileName consumeQueue文件名称
     * @return consumeQueue文件路径
     */
    public static String buildConsumeQueueFilePath(String topicName, Integer queueId, String consumeQueueFileName) {
        return CommonCache.getGlobalProperties().getGoldMqHome()
                + BrokerConstants.BASE_CONSUMER_QUEUE_PATH
                + topicName
                + BrokerConstants.SPLIT
                + queueId
                + BrokerConstants.SPLIT
                + consumeQueueFileName;
    }

    /**
     * 构建consumeQueue的文件路径
     *
     * @param topicName 消息主题
     * @return consumeQueue文件路径
     */
    public static String buildConsumeQueueBasePath(String topicName) {
        return CommonCache.getGlobalProperties().getGoldMqHome()
                + BrokerConstants.BASE_CONSUMER_QUEUE_PATH
                + topicName;
    }

    /**
     * 根据consumerqueue老的文件名生成新的文件名
     *
     * @param oldFileName 旧文件名
     * @return 新文件名
     */
    public static String incrConsumeQueueFileName(String oldFileName) {
        return incrCommitLogFileName(oldFileName);
    }

    /**
     * 根据commitLog老的文件名生成新的文件名
     *
     * @param oldFileName 旧文件名
     * @return 新文件名
     */
    public static String incrCommitLogFileName(String oldFileName) {
        if (oldFileName.length() != 8) {
            throw new IllegalArgumentException("fileName must has 8 chars");
        }
        Long fileIndex = Long.valueOf(oldFileName);
        fileIndex++;
        String newFileName = String.valueOf(fileIndex);
        int newFileNameLen = newFileName.length();
        int needFullLen = 8 - newFileNameLen;
        if (needFullLen < 0) {
            throw new RuntimeException("unKnow fileName error");
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < needFullLen; i++) {
            stringBuilder.append("0");
        }
        return stringBuilder.append(newFileName).toString();
    }
}
