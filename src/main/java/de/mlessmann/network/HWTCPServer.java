package de.mlessmann.network;

import de.mlessmann.hwserver.HWServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 01.05.16.
 * @author Life4YourGames
 */
public class HWTCPServer {

    private Logger log;
    private ServerSocket sSock;
    private HWServer master;
    private int port = 11901;
    private ArrayList<HWTCPClientWorker> ccList;
    private boolean isStopped = false;

    public HWTCPServer (HWServer hwServer) {

        log = hwServer.getLogger();
        master = hwServer;

    }

    public boolean setUp() {

        try {

            sSock = new ServerSocket(port);

            ccList = new ArrayList<HWTCPClientWorker>();

            return false;

        } catch (IOException ex) {

            log.severe("Unable to create ServerSocket! " + ex.toString());
            return true;
        }

    }

    public boolean runAction(HWTCPServerWorker worker) {

        try {

            Socket clientSock = sSock.accept();

            HWTCPClientWorker newCC = new HWTCPClientWorker(clientSock, this);

            ccList.add(newCC);

            newCC.start();

        } catch (IOException ex) {

            if (!isStopped) {
                log.severe("Unable to accept connections: " + ex.toString());
            }
            return true;

        }

        return false;

    }

    public synchronized void sendLog(Object sender, Level level, String message) {

        log.log(level, message);

    }

    public void interruptChildren() {

        ccList.stream().forEach(Thread::interrupt);

    }

    public void stopChildren() {

        ccList.stream().forEach(HWTCPClientWorker::closeConnection);

    }

    public void stop() {

        try {

            isStopped = true;
            sSock.close();
            //interruptChildren();
            stopChildren();

        } catch (IOException ex) {

            log.warning("Unable to close socket: " + ex.toString());

        }
    }

    public int getPort() {
        return port;
    }

    public HWServer getMaster() {
        return master;
    }

}
