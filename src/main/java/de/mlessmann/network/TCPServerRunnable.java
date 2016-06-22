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

    public TCPServerRunnable(HWTCPServer server, ServerSocket sock) {

        super();

        this.server = server;

        this.socket = sock;

    }


    public void run() {

        stopped = false;

        server.sendLog(this, Level.INFO, "Opening server on: " + socket.getLocalSocketAddress());

        while (!this.stopped) {

            try {

                Socket clientSock = socket.accept();

                HWTCPClientWorker newCC = new HWTCPClientWorker(clientSock, server);

                server.addClient(newCC);

                newCC.start();

            } catch (SSLException sslEx) {

                server.sendLog(this, Level.FINER, "Denying incoming connection: " + sslEx);

            } catch (IOException ex) {

                if (!server.isStopped()) {

                    server.sendLog(this, Level.SEVERE, "Unable to accept connections: " + ex.toString());

                }

                stopped = true;
                server.reportRunTermination(this);

            }

        }

    }

    @Override
    public String toString() {

        return "TCPRunable_" + socket.toString();

    }

}

