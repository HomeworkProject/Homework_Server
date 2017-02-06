package de.homeworkproject.server.network.filetransfer;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 09.11.16.
 */
public class FileTransferWorker extends Thread {

    private Socket socket;
    private FileTransferServer server;
    private int tokenSize;
    private boolean terminated;
    private int maxSize = -1;

    public FileTransferWorker(Socket socket, FileTransferServer server, int tokenSize) {
        this.socket = socket;
        this.server = server;
        this.tokenSize = tokenSize;
    }

    public FileTransferWorker setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    @Override
    public void run() {

        InputStream in;
        OutputStream out;
        try {
            socket.setSoTimeout(10000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "File transfer terminated: Unable to assign IO");
            kill();
            return;
        }

        byte[] token = new byte[tokenSize];

        try {
            server.getHWServer().onMessage(this, Level.FINER, "Reading token - Expected size: " + tokenSize);
            int count = 0;
            int currIndex = 0;
            while ((count = in.read(token, currIndex, tokenSize - currIndex)) > -1 && currIndex<tokenSize) {
                currIndex += count;
                server.getHWServer().onMessage(this, Level.FINEST, "Reading token - Expected size left: " + (tokenSize - currIndex));
            }
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINER, "Token not retrievable - Closing connection");
            kill();
            return;
        }
        server.getHWServer().onMessage(this, Level.FINER, "Authorizing transfer");
        Optional<FileTransferInfo> optFile = server.authorize(token);
        if (!optFile.isPresent()) {
            server.getHWServer().onMessage(this, Level.FINE, "Token not authorized - Closing connection");
            kill();
            return;
        }
        if (optFile.get().isIncoming())
            incoming(optFile.get(), in, out);
        else
            outgoing(optFile.get(), in, out);
    }

    private void incoming(FileTransferInfo i, InputStream in, OutputStream out) {

        File file = i.getTarget();

        //try {
            if (!file.exists()) {
                File dir = file.getAbsoluteFile().getParentFile();
                boolean s = (dir.isDirectory() || dir.mkdirs()) /*&& file.getAbsoluteFile().createNewFile()*/;
                if (!s) {
                    server.getHWServer().onMessage(this, Level.FINE, "Unable to create file: " + file.getPath());
                    kill();
                    return;
                }
            }
        /*} catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "Unable to store file: " + file.getPath() + ":" + e.getMessage());
            server.getHWServer().onException(this, Level.WARNING, e);
            kill();
            return;
        }*/
        try {
            out.write("ACCEPT".getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "Unable to send ready state - Closing connection");
            kill();
            return;
        }

        byte[] buffer = new byte[2048];
        int read = 0;
        int total = 0;
        boolean failed = false;

        try (FileOutputStream f = new FileOutputStream(file)) {
            while ((read = in.read(buffer)) > -1) {
                server.getHWServer().onMessage(this, Level.FINEST, "Writing... [" + total + "]");
                total += read;
                if (maxSize != -1 && total > maxSize)
                    throw new IOException("Max file size reached!");
                f.write(buffer, 0, read);
            }
            f.flush();
            if (total == 0) failed = true;
            server.getHWServer().onMessage(this, Level.FINE, "Received file of size: " + total);
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "Unable to retrieve file: IOE while r/w - Closing connection");
            if (!(e instanceof SocketTimeoutException))
                server.getHWServer().onException(this, Level.FINE, e);
            failed = true;
            terminated = true;
        }

        if (failed && !file.delete()) {
            server.getHWServer().onMessage(this, Level.WARNING, "Unable to delete discarded file remnants of: " + file.getPath());
        }
        kill();
    }

    private void outgoing(FileTransferInfo i, InputStream in, OutputStream out) {

        File file = i.getTarget();
        try (FileInputStream fIn = new FileInputStream(file)){
            byte[] buffer = new byte[2048];
            int count = 0;
            while ((count = fIn.read(buffer)) > -1) {
                out.write(buffer, 0, count);
                out.flush();
            }
        } catch (IOException e) {
            server.getHWServer().onMessage(this, Level.FINE, "Unable to transmit file: IOE while r/w - Closing connection");
            server.getHWServer().onException(this, Level.FINE, e);
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
