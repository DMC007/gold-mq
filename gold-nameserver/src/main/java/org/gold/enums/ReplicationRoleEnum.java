package org.gold.enums;

/**
 * @author zhaoxun
 * @date 2025/11/27
 */
public enum ReplicationRoleEnum {

    MASTER("master", "主从-主"),
    SLAVE("slave", "主从-从"),
    NODE("not_tail_node", "链路复制-非尾部节点"),
    TAIL_NODE("tail_node", "链路复制-尾部节点"),
    ;
    private String code;
    private String desc;

    ReplicationRoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReplicationRoleEnum of(String code) {
        for (ReplicationRoleEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
