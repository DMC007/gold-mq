package org.gold.model;

/**
 * @author zhaoxun
 * @date 2025/10/21
 * @description commitLog真实数据存储对象模型
 */
public class CommitLogMessageModel {

    /**
     * 真正的消息内容
     */
    private byte[] content;

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
