package de.homeworkproject.server.network.filetransfer;

import java.io.File;

/**
 * Created by Life4YourGames on 13.11.16.
 */
public class FileTransferInfo {

    public static FileTransferInfo of(boolean incoming, File target, String token, int ttl) {
        FileTransferInfo i = new FileTransferInfo();
        if (incoming)
            i.setIncoming();
        else
            i.setOutgoing();
        i.setTarget(target);
        i.setToken(token);
        i.setTTL(ttl);
        return i;
    }

    private boolean incoming = false;
    private File target = null;
    private String token = null;
    private int ttl = 0;

    public FileTransferInfo() {
        super();
    }

    public void setIncoming() {
        incoming = true;
    }

    public void setOutgoing() {
        incoming = false;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public boolean isOutgoing() {
        return !incoming;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public File getTarget() {
        return target;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setTTL(int ttl) {
        this.ttl = ttl;
    }

    public int getTTL() {
        return ttl;
    }
}

