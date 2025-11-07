package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description 拉取broker节点ip信息请求体
 */
public class PullBrokerIpDTO extends BaseNameServerRemoteDTO {
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
