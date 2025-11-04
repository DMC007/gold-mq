package org.gold.coder;

import org.gold.constants.BrokerConstants;

/**
 * @author zhaoxun
 * @date 2025/11/4
 */
public class TcpMsg {
    //魔数
    private short magic;
    //表示请求包的具体含义
    private int code;
    //消息长度
    private int len;
    //消息体
    private byte[] body;

    public TcpMsg(int code, byte[] body) {
        this.magic = BrokerConstants.DEFAULT_MAGIC_NUM;
        this.code = code;
        this.len = body.length;
        this.body = body;
    }

    public short getMagic() {
        return magic;
    }

    public void setMagic(short magic) {
        this.magic = magic;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
