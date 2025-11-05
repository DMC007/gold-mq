package org.gold.core;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.store.ServiceInstance;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public class InValidServiceRemoveTask implements Runnable {

    private static final Logger log = LogManager.getLogger(InValidServiceRemoveTask.class);

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(3);
                Map<String, ServiceInstance> serviceInstanceMap = CommonCache.getServiceInstanceManager().getServiceInstanceMap();
                log.info("serviceInstanceMap size:{}", serviceInstanceMap.size());
                long currentTime = System.currentTimeMillis();
                Iterator<String> iterator = serviceInstanceMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String reqId = iterator.next();
                    ServiceInstance serviceInstance = serviceInstanceMap.get(reqId);
                    //注册的时候这个字段值为null，等第一次心跳过后这个值就会更新
                    if (serviceInstance.getLastHeartBeatTime() == null) {
                        continue;
                    }
                    if (currentTime - serviceInstance.getLastHeartBeatTime() > 3000 * 3) {
                        log.info("remove invalid serviceInstance:{}", JSON.toJSONString(serviceInstance));
                        Channel channel = serviceInstance.getChannel();
                        if (channel != null && channel.isActive()) {
                            log.info("close channel:{}", channel);
                            channel.close();
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                log.error("InValidServiceRemoveTask error:{}", e.getMessage(), e);
            }
        }
    }
}
