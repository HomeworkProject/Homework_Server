package de.homeworkproject.server.tasks;

import de.homeworkproject.server.hwserver.HWServer;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.INFO;

/**
 * Created by Life4YourGames on 08.10.16.
 */
public class TaskManager {

    public static int INITPOOLSIZE = 1;

    private ScheduledThreadPoolExecutor executor;
    private HWServer server;

    public TaskManager(HWServer server) {
        executor = new ScheduledThreadPoolExecutor(INITPOOLSIZE);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(true);
        this.server = server;
    }

    public synchronized void setPoolSize(int poolSize) {
        server.onMessage(this, INFO, "Changing pool size: " + executor.getPoolSize() + " -> " + poolSize);
        executor.setCorePoolSize(poolSize);
    }

    public synchronized void schedule(ITask t) {
        server.onMessage(this, FINER, "Scheduling new task in " + t.getInterval() + ' ' + t.getTimeUnit().toString());
        t.reportTaskManager(this);
        executor.schedule(t, t.getInterval(), t.getTimeUnit());
    }

    public synchronized void shutdown(boolean forced) {
        if (!forced) {
            executor.shutdown();
        } else {
            executor.shutdownNow();
        }
    }

}
