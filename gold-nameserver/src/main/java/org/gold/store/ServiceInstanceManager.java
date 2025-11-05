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

    public void putIfExist(ServiceInstance serviceInstance) {
        ServiceInstance currentInstance = this.get(serviceInstance.getIp(), serviceInstance.getPort());
        if (currentInstance != null && currentInstance.getFirstRegistryTime() != null) {
            currentInstance.setLastHeartBeatTime(serviceInstance.getLastHeartBeatTime());
            serviceInstanceMap.put(serviceInstance.getIp() + ":" + serviceInstance.getPort(), currentInstance);
        } else {
            throw new RuntimeException("The previous heartbeat cache has been removed; please register again.");
        }
    }

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
