package org.gold.config;

import io.netty.util.internal.StringUtil;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * @author zhaoxun
 * @date 2025/10/20
 */
public class GlobalPropertiesLoader {

    public void loadProperties() {
        GlobalProperties globalProperties = new GlobalProperties();
        String goldMqHome = System.getenv(BrokerConstants.GOLD_MQ_HOME);
        if (StringUtil.isNullOrEmpty(goldMqHome)) {
            throw new IllegalArgumentException("GOLD_MQ_HOME is null");
        }
        globalProperties.setGoldMqHome(goldMqHome);
        Properties properties = new Properties();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(new File(goldMqHome + BrokerConstants.BROKER_PROPERTIES_PATH).toPath()))) {
            properties.load(in);
            //TODO 后期考虑用通用工具类解决
            globalProperties.setNameserverIp(properties.getProperty("nameserver.ip"));
            globalProperties.setNameserverPort(Integer.valueOf(properties.getProperty("nameserver.port")));
            globalProperties.setNameserverUser(properties.getProperty("nameserver.user"));
            globalProperties.setBrokerPort(Integer.valueOf(properties.getProperty("broker.port")));
            globalProperties.setNameserverPassword(properties.getProperty("nameserver.password"));
            globalProperties.setReBalanceStrategy(properties.getProperty("rebalance.strategy"));
            //集群相关属性
            globalProperties.setBrokerClusterGroup(properties.getProperty("broker.cluster.group"));
            globalProperties.setBrokerClusterMode(properties.getProperty("broker.cluster.mode"));
            globalProperties.setBrokerClusterRole(properties.getProperty("broker.cluster.role"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //本地缓存对象缓存配置属性
        CommonCache.setGlobalProperties(globalProperties);
    }
}
