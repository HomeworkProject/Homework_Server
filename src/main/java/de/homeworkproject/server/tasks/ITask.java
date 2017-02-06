package de.homeworkproject.server.tasks;

import java.util.concurrent.TimeUnit;

/**
 * Created by Life4YourGames on 08.10.16.
 */
public interface ITask extends Runnable {

    int getInterval();

    TimeUnit getTimeUnit();

    void reportTaskManager(TaskManager mgr);
}
