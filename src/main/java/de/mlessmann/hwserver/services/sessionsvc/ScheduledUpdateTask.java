package de.mlessmann.hwserver.services.sessionsvc;

import de.mlessmann.hwserver.HWServer;

/**
 * Created by Life4YourGames on 20.09.16.
 */
public class ScheduledUpdateTask implements Runnable {

    private HWServer server;

    public ScheduledUpdateTask(HWServer server) {
        this.server = server;
    }

    public void run() {
        try {
            server.autoUpgrade();
            Thread.sleep(2000);
            server.checkForUpdate();
            Thread.sleep(2000);
            server.scheduleUpdate();
        } catch (Exception e) {

        }
    }
}
