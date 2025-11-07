package org.gold.dto;

/**
 * @author zhaoxun
 * @date 2025/11/7
 */
public class StartSyncRespDTO extends BaseNameServerRemoteDTO {
    private boolean isSuccess;

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}
