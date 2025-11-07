package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class SlaveSyncRespDTO extends BaseNameServerRemoteDTO {
    private boolean syncSuccess;

    public boolean isSyncSuccess() {
        return syncSuccess;
    }

    public void setSyncSuccess(boolean syncSuccess) {
        this.syncSuccess = syncSuccess;
    }
}
