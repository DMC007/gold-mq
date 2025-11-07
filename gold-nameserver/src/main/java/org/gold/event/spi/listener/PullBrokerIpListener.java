package org.gold.event.spi.listener;

import com.alibaba.fastjson2.JSON;
import org.gold.cache.CommonCache;
import org.gold.coder.TcpMsg;
import org.gold.dto.PullBrokerIpRespDTO;
import org.gold.enums.BrokerRegistryRoleEnum;
import org.gold.enums.NameServerResponseCode;
import org.gold.enums.RegistryTypeEnum;
import org.gold.event.Listener;
import org.gold.event.model.PullBrokerIpEvent;
import org.gold.store.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class PullBrokerIpListener implements Listener<PullBrokerIpEvent> {
    @Override
    public void onReceive(PullBrokerIpEvent event) throws Exception {
        String pullRole = event.getRole();
        PullBrokerIpRespDTO pullBrokerIpRespDTO = new PullBrokerIpRespDTO();
        List<String> addressList = new ArrayList<>();
        List<String> masterAddressList = new ArrayList<>();
        List<String> slaveAddressList = new ArrayList<>();
        Map<String, ServiceInstance> serviceInstanceMap = CommonCache.getServiceInstanceManager().getServiceInstanceMap();
        for (String reqId : serviceInstanceMap.keySet()) {
            ServiceInstance serviceInstance = serviceInstanceMap.get(reqId);
            //serviceInstance不止包括broker信息，这里仅处理broker的信息
            if (!RegistryTypeEnum.BROKER.getCode().equals(serviceInstance.getRegistryType())) {
                continue;
            }
            Map<String, Object> brokerAttrs = serviceInstance.getAttrs();
            String group = (String) brokerAttrs.getOrDefault("group", "");
            //选中broker实体后，再对角色进行区分判断
            if (group.equals(event.getBrokerClusterGroup())) {
                String role = (String) brokerAttrs.get("role");
                if (BrokerRegistryRoleEnum.MASTER.getCode().equals(pullRole)
                        && BrokerRegistryRoleEnum.MASTER.getCode().equals(role)) {
                    masterAddressList.add(serviceInstance.getIp() + ":" + serviceInstance.getPort());
                } else if (BrokerRegistryRoleEnum.SLAVE.getCode().equals(pullRole)
                        && BrokerRegistryRoleEnum.SLAVE.getCode().equals(role)) {
                    slaveAddressList.add(serviceInstance.getIp() + ":" + serviceInstance.getPort());
                } else if (BrokerRegistryRoleEnum.SINGLE.getCode().equals(pullRole)
                        && BrokerRegistryRoleEnum.SINGLE.getCode().equals(role)) {
                    addressList.add(serviceInstance.getIp() + ":" + serviceInstance.getPort());
                }
            }
        }
        //防重复处理
        pullBrokerIpRespDTO.setAddressList(addressList.stream().distinct().toList());
        pullBrokerIpRespDTO.setMasterAddressList(masterAddressList.stream().distinct().toList());
        pullBrokerIpRespDTO.setSlaveAddressList(slaveAddressList.stream().distinct().toList());
        pullBrokerIpRespDTO.setMsgId(event.getMsgId());
        TcpMsg tcpMsg = new TcpMsg(NameServerResponseCode.PULL_BROKER_ADDRESS_SUCCESS.getCode(), JSON.toJSONBytes(pullBrokerIpRespDTO));
        event.getChannelHandlerContext().writeAndFlush(tcpMsg);
    }
}
