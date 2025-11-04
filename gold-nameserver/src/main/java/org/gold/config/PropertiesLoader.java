package org.gold.config;

import io.netty.util.internal.StringUtil;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class PropertiesLoader {

    private Properties properties = new Properties();

    public void loadProperties() throws IOException {
        String goldMqHome = System.getenv(BrokerConstants.GOLD_MQ_HOME);
        if (StringUtil.isNullOrEmpty(goldMqHome)) {
            throw new IllegalArgumentException("GOLD_MQ_HOME is null");
        }
        properties.load(new FileInputStream(new File(goldMqHome + "/config/nameserver.properties")));
        NameserverProperties nameserverProperties = new NameserverProperties();
        nameserverProperties.setNameserverUser(getStr("nameserver.user"));
        nameserverProperties.setNameserverPwd(getStr("nameserver.password"));
        nameserverProperties.setNameserverPort(getInt("nameserver.port"));
        nameserverProperties.print();
        CommonCache.setNameserverProperties(nameserverProperties);
    }

    private String getStr(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("config param：" + key + "not exist");
        }
        return value;
    }

    private Integer getInt(String key) {
        return Integer.valueOf(getStr(key));
    }
}
