package de.mlessmann.network;

import de.mlessmann.hwserver.HWServer;

import java.net.ServerSocket;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 01.05.16.
 * @author Life4YourGames
 */
public class HWTCPServerWorker extends Thread {

    private ServerSocket sSock;
    private Logger log;
    private int port = 11901;
    private HWTCPServer hwTCPServer;
    public boolean terminated = true;

    public HWTCPServerWorker (HWServer hwServer) {

        super();
        this.hwTCPServer = new HWTCPServer(hwServer);
        log = hwServer.getLogger();

    }

    @Override
    public void run() {
        terminated = false;

        terminated = hwTCPServer.setUp();

        if (!terminated) {
            log.info("Opened connection on port: " + hwTCPServer.getPort());
        }

            while (!terminated) {

                terminated = hwTCPServer.runAction(this);

            }
    }

    public void interruptChildren() {

        hwTCPServer.interruptChildren();

    }

    public synchronized void stopServer() {

        hwTCPServer.stop();
        //this.interrupt();

    }

}
