package org.gold.config;

import com.alibaba.fastjson2.JSON;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;
import org.gold.model.ConsumerQueueOffsetModel;
import org.gold.utils.FileContentUtil;

import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public class ConsumerQueueOffsetLoader {

    private static final Logger log = LogManager.getLogger(ConsumerQueueOffsetLoader.class);

    private String filePath;

    public void loadProperties() {
        GlobalProperties globalProperties = CommonCache.getGlobalProperties();
        String basePath = globalProperties.getGoldMqHome();
        if (StringUtil.isNullOrEmpty(basePath)) {
            throw new IllegalArgumentException("GOLD_MQ_HOME is invalid!");
        }
        filePath = basePath + BrokerConstants.CONSUMER_QUEUE_OFFSET_FILE;
        String fileContent = FileContentUtil.readFile(filePath);
        ConsumerQueueOffsetModel consumerQueueOffsetModel = JSON.parseObject(fileContent, ConsumerQueueOffsetModel.class);
        CommonCache.setConsumerQueueOffsetModel(consumerQueueOffsetModel);
    }

    public void startRefreshConsumerQueueOffsetTask() {
        CommonThreadPoolConfig.refreshConsumeQueueOffsetExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(BrokerConstants.DEFAULT_REFRESH_CONSUME_QUEUE_OFFSET_TIME_STEP);
                        ConsumerQueueOffsetModel consumerQueueOffsetModel = CommonCache.getConsumerQueueOffsetModel();
                        FileContentUtil.writeFile(filePath, JSON.toJSONString(consumerQueueOffsetModel));
                    } catch (Exception e) {
                        log.error("refreshConsumerQueueOffsetTask error:{}", e.getMessage(), e);
                    }
                }
            }
        });
    }
}
