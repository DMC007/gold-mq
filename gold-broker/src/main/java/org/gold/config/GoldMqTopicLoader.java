package org.gold.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;
import org.gold.model.GoldMqTopicModel;
import org.gold.utils.FileContentUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description 负责将mq的主题配置信息加载到内存中
 */
public class GoldMqTopicLoader {

    private static final Logger log = LogManager.getLogger(GoldMqTopicLoader.class);

    private String filePath;

    public void loadProperties() {
        GlobalProperties globalProperties = CommonCache.getGlobalProperties();
        String basePath = globalProperties.getGoldMqHome();
        if (StringUtil.isNullOrEmpty(basePath)) {
            throw new IllegalArgumentException("GOLD_MQ_HOME is invalid!");
        }
        filePath = basePath + BrokerConstants.TOPIC_CONFIG_FILE;
        String fileContent = FileContentUtil.readFile(filePath);
        List<GoldMqTopicModel> goldMqTopicModels = JSON.parseArray(fileContent, GoldMqTopicModel.class);
        //放入公共本地缓存
        CommonCache.setGoldMqTopicModelList(goldMqTopicModels);
    }

    /**
     * 开启一个刷新内存到磁盘的任务
     * 异步线程
     * 每3秒将内存中的配置刷新到磁盘里面
     */
    public void startRefreshGoldMqTopicInfoTask() {
        CommonThreadPoolConfig.refreshGoldMqTopicExecutor.execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(BrokerConstants.DEFAULT_REFRESH_MQ_TOPIC_TIME_STEP);
                        List<GoldMqTopicModel> goldMqTopicModelList = CommonCache.getGoldMqTopicModelList();
                        FileContentUtil.writeFile(filePath, JSON.toJSONString(goldMqTopicModelList, JSONWriter.Feature.PrettyFormat));
                    } catch (InterruptedException e) {
                        //打印错误日志，考虑到如果这里抛出异常，线程结束了，那么就刷新磁盘的任务就终止了
                        log.error("refreshGoldMqTopicInfoTask error:{}", e.getMessage(), e);
                    }
                }
            }
        });
    }
}
