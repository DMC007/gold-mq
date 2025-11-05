package org.gold.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/11/5
 * @description 服务注册请求
 */
public class ServiceRegistryReqDTO extends BaseNameServerRemoteDTO {

    /**
     * 节点的注册类型，方便统计数据使用
     *
     * @see org.gold.enums.RegistryTypeEnum
     */
    private String registryType;
    private String user;
    private String password;
    private String ip;
    private Integer port;
    private Map<String, Object> attrs = new HashMap<>();

    public String getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }
}
