package de.mlessmann.hwserver;

import java.io.IOException;

/**
 * Created by Life4YourGames on 09.06.16.
 */
public class HWServerRunnable implements Runnable {

    private HWServer server;
    public Exception lastError;

    public HWServerRunnable(HWServer myServer) {
        server = myServer;
    }

    public void run() {

        try {
            server.preInitialize();
            server.start();
        } catch (IOException ex) {
            lastError = ex;
        }

    }

}
