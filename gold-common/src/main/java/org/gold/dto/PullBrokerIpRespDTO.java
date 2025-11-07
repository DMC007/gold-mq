package org.gold.dto;

import java.util.List;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description broker的ip列表响应实体
 */
public class PullBrokerIpRespDTO extends BaseNameServerRemoteDTO {
    private List<String> addressList;

    private List<String> masterAddressList;

    private List<String> slaveAddressList;

    public List<String> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<String> addressList) {
        this.addressList = addressList;
    }

    public List<String> getMasterAddressList() {
        return masterAddressList;
    }

    public void setMasterAddressList(List<String> masterAddressList) {
        this.masterAddressList = masterAddressList;
    }

    public List<String> getSlaveAddressList() {
        return slaveAddressList;
    }

    public void setSlaveAddressList(List<String> slaveAddressList) {
        this.slaveAddressList = slaveAddressList;
    }
}
