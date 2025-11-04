package org.gold.constants;

/**
 * @author zhaoxun
 * @date 2025/10/20
 */
public class BrokerConstants {
    public static final String GOLD_MQ_HOME = "GOLD_MQ_HOME";
    public static final String BASE_COMMIT_PATH = "/commitlog/";
    public static final String BASE_CONSUMER_QUEUE_PATH = "/consumequeue/";
    public static final String BROKER_PROPERTIES_PATH = "/config/broker.properties";
    public static final String TOPIC_CONFIG_FILE = "/config/goldmq-topic.json";
    public static final String CONSUMER_QUEUE_OFFSET_FILE = "/config/consumerqueue-offset.json";
    public static final String SPLIT = "/";
    public static final Integer DEFAULT_REFRESH_MQ_TOPIC_TIME_STEP = 3;
    public static final Integer DEFAULT_REFRESH_CONSUME_QUEUE_OFFSET_TIME_STEP = 1;
    public static final Integer COMMIT_LOG_DEFAULT_MMAP_SIZE = 1024 * 1024; //1mb单位，方便讲解使用;
    public static final Integer COMSUMERQUEUE_DEFAULT_MMAP_SIZE = 1024 * 1024;
    public static final int CONSUME_QUEUE_EACH_MSG_SIZE = 16;
    public static final short DEFAULT_MAGIC_NUM = 17671;
}
