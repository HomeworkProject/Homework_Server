package de.mlessmann.network.filetransfer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 09.11.16.
 */
public class FileTransferWorker extends Thread {

    private Socket socket;
    private FileTransferServerRunnable server;
    private int tokenSize;
    private boolean terminated;

    public FileTransferWorker(Socket socket, FileTransferServerRunnable server, int tokenSize) {
        this.socket = socket;
        this.server = server;
        this.tokenSize = tokenSize;
    }

    @Override
    public void run() {

        InputStream in;
        OutputStream out;
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "File transfer terminated: Unable to assign IO");
            kill();
            return;
        }

        byte[] token = new byte[tokenSize];
        try {
            if (!(in.read(token, 0, tokenSize) == tokenSize)) {
                server.getHWServer().onMessage(this, Level.FINER, "Token size didn't match - Closing connection");
                kill();
                return;
            }
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINER, "Token not retrievable - Closing connection");
            kill();
            return;
        }
        Optional<File> optFile = server.authorize(token);
        if (!optFile.isPresent()) {
            server.getHWServer().onMessage(this, Level.FINER, "Token not authorized - Closing connection");
            kill();
            return;
        }
        File file = optFile.get();

        try {
            if (!file.exists()) {
                boolean s = file.getAbsoluteFile().getParentFile().mkdirs() && file.getAbsoluteFile().createNewFile();
                if (!s) {
                    server.getHWServer().onMessage(this, Level.FINE, "Unable to create file: " + file.getPath());
                    kill();
                    return;
                }
            }
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "Unable to store file: " + file.getPath() + ":" + e.getMessage());
            kill();
            return;
        }
        try {
            out.write("ACCEPT".getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "Unable to send ready state - Closing connection");
            kill();
            return;
        }

        byte[] buffer = new byte[2048];
        int read = 0;

        while (!terminated) {
            try (FileOutputStream f = new FileOutputStream(file);) {
                while ((read = in.read(buffer))>=-1){
                    f.write(buffer, 0, read);
                }
            } catch (IOException e) {
                server.getHWServer().onMessage(this, Level.FINE, "Unable to retrieve file: IOE while r/w - Closing connection");
                kill();
                return;
            }
        }
        kill();
    }

    public synchronized void kill() {
        try {
            socket.close();
        } catch (IOException e) {
            //
        }
        terminated = true;
        server.onDone(this);
    }
}
