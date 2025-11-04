package org.gold.core;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhaoxun
 * @date 2025/10/24
 * @description commitlog的mmap映射对象的管理器
 */
public class CommitLogMMapFileModelManager {
    /**
     * key:主题名称，value:文件的mMap对象
     */
    private Map<String, CommitLogMMapFileModel> commitLogMMapFileModelMap = new HashMap<>();

    public void put(String topic, CommitLogMMapFileModel mapFileModel) {
        commitLogMMapFileModelMap.put(topic, mapFileModel);
    }

    public CommitLogMMapFileModel get(String topic) {
        return commitLogMMapFileModelMap.get(topic);
    }
}
