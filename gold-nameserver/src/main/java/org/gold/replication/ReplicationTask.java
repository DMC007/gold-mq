package org.gold.replication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhaoxun
 * @date 2025/12/1
 */
public abstract class ReplicationTask {

    private static final Logger log = LogManager.getLogger(ReplicationTask.class);

    private String taskName;

    public ReplicationTask(String taskName) {
        this.taskName = taskName;
    }

    public abstract void startTask();

    public void startTaskAsync() {
        new Thread(() -> {
            log.info("start task:{}", taskName);
            startTask();
        }, taskName).start();
    }
}
