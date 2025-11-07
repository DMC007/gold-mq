package org.gold.event.model;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description broker拉取broker ip列表事件
 */
public class PullBrokerIpEvent extends Event {
    private String role;
    private String brokerClusterGroup;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBrokerClusterGroup() {
        return brokerClusterGroup;
    }

    public void setBrokerClusterGroup(String brokerClusterGroup) {
        this.brokerClusterGroup = brokerClusterGroup;
    }
}
