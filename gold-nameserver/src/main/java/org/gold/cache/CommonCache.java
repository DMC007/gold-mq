package org.gold.cache;

import io.netty.channel.Channel;
import org.gold.config.NameserverProperties;
import org.gold.config.PropertiesLoader;
import org.gold.store.ServiceInstanceManager;

/**
 * @author zhaoxun
 * @date 2025/11/4
 * @description 管理中心【可看作spring容器管理一样】
 */
public class CommonCache {

    private static PropertiesLoader propertiesLoader = new PropertiesLoader();
    private static NameserverProperties nameserverProperties = new NameserverProperties();
    private static ServiceInstanceManager serviceInstanceManager = new ServiceInstanceManager();
    private static Channel connectNodeChannel = null;

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

    public static ServiceInstanceManager getServiceInstanceManager() {
        return serviceInstanceManager;
    }

    public static void setServiceInstanceManager(ServiceInstanceManager serviceInstanceManager) {
        CommonCache.serviceInstanceManager = serviceInstanceManager;
    }

    public static Channel getConnectNodeChannel() {
        return connectNodeChannel;
    }

    public static void setConnectNodeChannel(Channel connectNodeChannel) {
        CommonCache.connectNodeChannel = connectNodeChannel;
    }
}
