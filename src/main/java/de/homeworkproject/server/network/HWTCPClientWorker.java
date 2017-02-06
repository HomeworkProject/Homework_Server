package de.homeworkproject.server.network;

import java.net.Socket;

/**
 * Created by mark332 on 08.05.2016.
 * @author Life4YourGames
 */
public class HWTCPClientWorker extends Thread {

    private HWTCPClientHandler clientHandler;
    private boolean terminated = true;

    public HWTCPClientWorker(Socket clientSock, HWTCPServer tcpServer) {

        this.clientHandler = new HWTCPClientHandler(clientSock, tcpServer);

    }

    @Override
    public void run() {
        terminated = false;
        terminated = !clientHandler.setUp();

        while (!terminated) {
            //False -> runAction unsuccessful -> Thread can be terminated
            terminated = clientHandler.runAction();
        }
    }

    /**
     * Whether or not this is scheduled to be terminated/or even already has been terminated
     * @return private field->terminated
     * @see #terminated
     */
    public synchronized boolean isTerminated() {
        return terminated;
    }

    public synchronized void closeConnection () {
        clientHandler.closeConnection();
    }
}
