package de.mlessmann.network;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 07.06.16.
 */
public class TCPServerRunnable implements Runnable {

    private HWTCPServer server;
    private ServerSocket socket;
    private boolean stopped;
    private String logClass;

    public TCPServerRunnable(HWTCPServer server, ServerSocket sock, String logClass) {
        super();
        this.server = server;
        this.socket = sock;
        this.logClass = logClass;
    }


    public void run() {
        stopped = false;
        server.sendLog(this, Level.INFO, logClass + " Opening server on: " + socket.getLocalSocketAddress());
        while (!this.stopped) {
            try {
                Socket clientSock = socket.accept();
                HWTCPClientWorker newCC = new HWTCPClientWorker(clientSock, server);
                server.addClient(newCC);
                newCC.start();
                server.sendLog(this, Level.FINE, logClass + " New connection: " + clientSock.getRemoteSocketAddress());
            } catch (SSLException sslEx) {
                server.sendLog(this, Level.FINER, logClass + " Denying incoming connection: " + sslEx);
            } catch (IOException ex) {
                if (!server.isStopped()) {
                    server.sendLog(this, Level.SEVERE, logClass + " Unable to accept connections: " + ex.toString());
                }
                stopped = true;
                server.reportRunTermination(this);
            }
        }
    }

    @Override
    public String toString() {
        return "TCPRunnable_" + socket.toString();
    }

}

