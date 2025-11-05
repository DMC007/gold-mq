package org.gold.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaoxun
 * @date 2025/11/5
 */
public class ServiceInstanceManager {
    //key: ip:port
    private Map<String, ServiceInstance> serviceInstanceMap = new ConcurrentHashMap<>();

    public void put(ServiceInstance serviceInstance) {
        serviceInstanceMap.put(serviceInstance.getIp() + ":" + serviceInstance.getPort(), serviceInstance);
    }

    public ServiceInstance get(String brokerIp, Integer brokerPort) {
        return serviceInstanceMap.get(brokerIp + ":" + brokerPort);
    }

    public ServiceInstance get(String reqId) {
        return serviceInstanceMap.get(reqId);
    }

    public ServiceInstance remove(String key) {
        return serviceInstanceMap.remove(key);
    }

    public Map<String, ServiceInstance> getServiceInstanceMap() {
        return serviceInstanceMap;
    }
}
