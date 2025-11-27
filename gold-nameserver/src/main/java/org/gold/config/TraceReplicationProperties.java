package org.gold.config;

/**
 * @author zhaoxun
 * @date 2025/11/27
 * @description 链路化方式同步配置
 */
public class TraceReplicationProperties {

    private String nextNode;

    private Integer port;

    public String getNextNode() {
        return nextNode;
    }

    public void setNextNode(String nextNode) {
        this.nextNode = nextNode;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
