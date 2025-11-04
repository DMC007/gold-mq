package org.gold.config;

/**
 * @author zhaoxun
 * @date 2025/10/20
 */
public class GlobalProperties {
    /**
     * nameserver属性
     */
    private String nameserverIp;
    private Integer nameserverPort;
    private String nameserverUser;
    private String nameserverPassword;
    private Integer brokerPort;
    /**
     * 重平衡策略
     */
    private String reBalanceStrategy;

    /**
     * 读取环境变量中配置的mq存储绝对路径地址
     */
    private String goldMqHome;

    public String getNameserverIp() {
        return nameserverIp;
    }

    public void setNameserverIp(String nameserverIp) {
        this.nameserverIp = nameserverIp;
    }

    public Integer getNameserverPort() {
        return nameserverPort;
    }

    public void setNameserverPort(Integer nameserverPort) {
        this.nameserverPort = nameserverPort;
    }

    public String getNameserverUser() {
        return nameserverUser;
    }

    public void setNameserverUser(String nameserverUser) {
        this.nameserverUser = nameserverUser;
    }

    public String getNameserverPassword() {
        return nameserverPassword;
    }

    public void setNameserverPassword(String nameserverPassword) {
        this.nameserverPassword = nameserverPassword;
    }

    public Integer getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(Integer brokerPort) {
        this.brokerPort = brokerPort;
    }

    public String getReBalanceStrategy() {
        return reBalanceStrategy;
    }

    public void setReBalanceStrategy(String reBalanceStrategy) {
        this.reBalanceStrategy = reBalanceStrategy;
    }

    public String getGoldMqHome() {
        return goldMqHome;
    }

    public void setGoldMqHome(String goldMqHome) {
        this.goldMqHome = goldMqHome;
    }
}
