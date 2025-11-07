package org.gold.slave;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.coder.TcpMsg;
import org.gold.dto.StartSyncReqDTO;
import org.gold.enums.BrokerEventCode;
import org.gold.event.EventBus;
import org.gold.remote.BrokerNettyRemoteClient;

import java.util.UUID;

/**
 * @author zhaoxun
 * @date 2025/11/7
 * @description 从节点同步服务
 */
public class SlaveSyncService {

    private static final Logger log = LogManager.getLogger(SlaveSyncService.class);

    private BrokerNettyRemoteClient brokerNettyRemoteClient;

    public boolean connectMasterBroker(String address) {
        String[] addressArr = address.split(":");
        String ip = addressArr[0];
        Integer port = Integer.parseInt(addressArr[1]);
        try {
            brokerNettyRemoteClient = new BrokerNettyRemoteClient(ip, port);
            brokerNettyRemoteClient.buildConnection(new SlaveSyncServerHandler(new EventBus("slave-sync-eventbus")));
            return true;
        } catch (Exception e) {
            log.error("connect master broker error", e);
        }
        return false;
    }

    public void sendStartSyncMsg() {
        StartSyncReqDTO reqDTO = new StartSyncReqDTO();
        reqDTO.setMsgId(UUID.randomUUID().toString());
        TcpMsg tcpMsg = new TcpMsg(BrokerEventCode.START_SYNC_MSG.getCode(), JSON.toJSONBytes(reqDTO));
        TcpMsg startSyncMsgRes = brokerNettyRemoteClient.sendSyncMsg(tcpMsg, reqDTO.getMsgId());
        log.info("start sync msg res: {}", JSON.toJSONString(startSyncMsgRes));
    }
}
