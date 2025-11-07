package org.gold.event.spi.listener;

import org.gold.event.Listener;
import org.gold.event.model.PullBrokerIpEvent;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class PullBrokerIpListener implements Listener<PullBrokerIpEvent> {
    @Override
    public void onReceive(PullBrokerIpEvent event) throws Exception {
        //TODO broker拉取broker ip列表
    }
}
