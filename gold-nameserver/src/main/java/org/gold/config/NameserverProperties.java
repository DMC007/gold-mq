package org.gold.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class NameserverProperties {

    private static final Logger log = LogManager.getLogger(NameserverProperties.class);

    private String nameserverUser;
    private String nameserverPwd;
    private Integer nameserverPort;
    private String replicationMode;
    private TraceReplicationProperties traceReplicationProperties;
    private MasterSlaveReplicationProperties masterSlaveReplicationProperties;

    public String getNameserverUser() {
        return nameserverUser;
    }

    public void setNameserverUser(String nameserverUser) {
        this.nameserverUser = nameserverUser;
    }

    public String getNameserverPwd() {
        return nameserverPwd;
    }

    public void setNameserverPwd(String nameserverPwd) {
        this.nameserverPwd = nameserverPwd;
    }

    public Integer getNameserverPort() {
        return nameserverPort;
    }

    public void setNameserverPort(Integer nameserverPort) {
        this.nameserverPort = nameserverPort;
    }

    public String getReplicationMode() {
        return replicationMode;
    }

    public void setReplicationMode(String replicationMode) {
        this.replicationMode = replicationMode;
    }

    public TraceReplicationProperties getTraceReplicationProperties() {
        return traceReplicationProperties;
    }

    public void setTraceReplicationProperties(TraceReplicationProperties traceReplicationProperties) {
        this.traceReplicationProperties = traceReplicationProperties;
    }

    public MasterSlaveReplicationProperties getMasterSlaveReplicationProperties() {
        return masterSlaveReplicationProperties;
    }

    public void setMasterSlaveReplicationProperties(MasterSlaveReplicationProperties masterSlaveReplicationProperties) {
        this.masterSlaveReplicationProperties = masterSlaveReplicationProperties;
    }

    public void print() {
        log.info(JSON.toJSONString(this, JSONWriter.Feature.PrettyFormat));
    }
}
