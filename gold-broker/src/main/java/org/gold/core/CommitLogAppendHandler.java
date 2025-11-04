package org.gold.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gold.cache.CommonCache;
import org.gold.constants.BrokerConstants;
import org.gold.dto.MessageDTO;

import java.io.IOException;

/**
 * @author zhaoxun
 * @date 2025/10/24
 */
public class CommitLogAppendHandler {
    private static final Logger log = LogManager.getLogger(CommitLogAppendHandler.class);

    public void prepareMMapLoading(String topicName) throws IOException {
        CommitLogMMapFileModel commitLogMMapFileModel = new CommitLogMMapFileModel();
        //文件映射
        commitLogMMapFileModel.loadFileToMMap(topicName, 0, BrokerConstants.COMMIT_LOG_DEFAULT_MMAP_SIZE);
        CommonCache.getCommitLogMMapFileModelManager().put(topicName, commitLogMMapFileModel);
    }

    //TODO 消息追加V1
    public void appendMessage(MessageDTO messageDTO) throws IOException {
        CommitLogMMapFileModel commitLogMMapFileModel = CommonCache.getCommitLogMMapFileModelManager().get(messageDTO.getTopic());
        if(commitLogMMapFileModel == null) {
            throw new RuntimeException("topic is invalid!");
        }
        commitLogMMapFileModel.writeContent(messageDTO, true);
    }
}
