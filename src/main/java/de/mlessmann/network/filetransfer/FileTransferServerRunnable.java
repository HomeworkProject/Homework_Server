package de.mlessmann.network.filetransfer;

import de.mlessmann.common.L4YGRandom;
import de.mlessmann.hwserver.HWServer;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 09.11.16.
 */
public class FileTransferServerRunnable implements Runnable {

    public final int defaultTTL = 32;

    private HWServer server;
    private Map<String, File> tokenDatabase;
    private Map<String, Integer> tokenTTL;
    private List<FileTransferWorker> transfers;
    private ServerSocket socket;
    private int tokenSize;
    private String t;
    private int tokenByteSize;

    private boolean stopped;
    private boolean enabled;

    public FileTransferServerRunnable(ServerSocket socket, int tokenSize, HWServer server) {
        tokenDatabase = new HashMap<String, File>();
        tokenTTL = new HashMap<String, Integer>();
        transfers = new ArrayList<FileTransferWorker>();
        this.socket = socket;
        this.tokenSize = tokenSize;
        this.t = new String(new char[tokenSize]);
        this.tokenByteSize = this.t.getBytes(Charset.forName("UTF-8")).length;
        this.server = server;
        enabled = this.socket!=null;
        stopped = true;
    }

    @Override
    public void run() {
        if (socket == null) {
            stopped = true;
            return;
        }
        stopped = false;

        server.onMessage(this, Level.INFO, "Opening file transfer server on: " + socket.getLocalSocketAddress());

        while (!this.stopped) {

            try {
                Socket clientSock = socket.accept();
                FileTransferWorker newCC = new FileTransferWorker(clientSock, this, tokenByteSize);
                transfers.add(newCC);
                newCC.start();
                server.onMessage(this, Level.FINE, "New ft-connection: " + clientSock.getRemoteSocketAddress());
            } catch (SSLException sslEx) {

                server.onMessage(this, Level.FINER, "Denying incoming connection: " + sslEx);

            } catch (IOException ex) {

                if (!stopped) {
                    server.onMessage(this, Level.SEVERE, "Unable to accept connections: " + ex.toString());
                }
                stopped = true;
                server.onMessage(this, Level.INFO, "Runnable ended: " + this.toString());
            }

        }
    }

    /**
     * Request a new token to approve a file transfer
     * @param destination the file that should be written to
     * @return Optional of the token, empty means denied
     */
    public synchronized Optional<String> requestTransferApproval(File destination) {
        //TODO: Safety mechanisms ?
        L4YGRandom.initRndIfNotAlready();

        String token;
        do {
            token = L4YGRandom.genRandomAlphaNumString(tokenSize);
        } while (tokenTTL.containsKey(token));
        return Optional.ofNullable(token);
    }

    public synchronized Optional<File> authorize(byte[] bytes) {
        return Optional.ofNullable(tokenDatabase.get(new String(bytes)));
    }

    protected synchronized void onDone(FileTransferWorker w) {
        transfers.remove(w);
    }

    public HWServer getHWServer() {
        return server;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
