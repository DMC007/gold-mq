package org.gold.config;

/**
 * @author zhaoxun
 * @date 2025/11/27
 */
public class MasterSlaveReplicationProperties {
    private String master;

    private String role;

    /**
     * @see org.gold.enums.MasterSlaveReplicationTypeEnum
     */
    private String type;

    private Integer port;

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
