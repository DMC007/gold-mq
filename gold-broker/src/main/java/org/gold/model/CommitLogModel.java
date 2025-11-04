package org.gold.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaoxun
 * @date 2025/10/21
 */
public class CommitLogModel {
    /**
     * 最新的commitLog文件名称
     */
    private String fileName;
    /**
     * 最新commitLog文件写入数据的地址
     */
    private AtomicInteger offset;
    /**
     * commitLog文件写入的最大上限体积
     */
    private Long offsetLimit;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public AtomicInteger getOffset() {
        return offset;
    }

    public void setOffset(AtomicInteger offset) {
        this.offset = offset;
    }

    public Long getOffsetLimit() {
        return offsetLimit;
    }

    public void setOffsetLimit(Long offsetLimit) {
        this.offsetLimit = offsetLimit;
    }

    public Long countDiff() {
        return this.offsetLimit - this.offset.get();
    }

    @Override
    public String toString() {
        return "CommitLogModel{" +
                "fileName='" + fileName + '\'' +
                ", offset=" + offset +
                ", offsetLimit=" + offsetLimit +
                '}';
    }
}
