package org.gold;

import org.gold.cache.CommonCache;
import org.gold.core.NameServerStarter;

import java.io.IOException;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class NameServerStartUp {

    private static NameServerStarter nameServerStarter;
    public static void main(String[] args) throws IOException, InterruptedException {
        CommonCache.getPropertiesLoader().loadProperties();
        nameServerStarter = new NameServerStarter(CommonCache.getNameserverProperties().getNameserverPort());
        nameServerStarter.startServer();
    }
}
