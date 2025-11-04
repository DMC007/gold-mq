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
    //TODO 还有其他属性后续添加


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

    public void print() {
        log.info(JSON.toJSONString(this, JSONWriter.Feature.PrettyFormat));
    }
}
