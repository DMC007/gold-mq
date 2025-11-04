package org.gold.cache;

import org.gold.config.NameserverProperties;
import org.gold.config.PropertiesLoader;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class CommonCache {

    private static PropertiesLoader propertiesLoader = new PropertiesLoader();
    private static NameserverProperties nameserverProperties = new NameserverProperties();

    public static NameserverProperties getNameserverProperties() {
        return nameserverProperties;
    }

    public static void setNameserverProperties(NameserverProperties nameserverProperties) {
        CommonCache.nameserverProperties = nameserverProperties;
    }

    public static PropertiesLoader getPropertiesLoader() {
        return propertiesLoader;
    }

    public static void setPropertiesLoader(PropertiesLoader propertiesLoader) {
        CommonCache.propertiesLoader = propertiesLoader;
    }
}
