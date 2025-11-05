package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/5
 * @description 请求nameserver服务响应码
 */
public enum NameServerResponseCode {

    ERROR_USER_OR_PASSWORD(1001, "账号验证异常"),
    UN_REGISTRY_SERVICE(1002, "服务正常下线"),
    REGISTRY_SUCCESS(1003, "注册成功"),
    HEART_BEAT_SUCCESS(1004, "心跳ACK"),
    PULL_BROKER_ADDRESS_SUCCESS(1005, "拉broker地址成功");

    private Integer code;
    private String desc;

    NameServerResponseCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
