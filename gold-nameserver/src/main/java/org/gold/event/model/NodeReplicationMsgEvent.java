package org.gold.event.model;

import org.gold.store.ServiceInstance;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public class NodeReplicationMsgEvent extends Event {
    private Integer type;
    private ServiceInstance serviceInstance;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }
}
