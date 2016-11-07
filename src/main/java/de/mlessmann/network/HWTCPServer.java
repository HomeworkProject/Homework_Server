package de.mlessmann.network;

import de.mlessmann.config.ConfigNode;
import de.mlessmann.hwserver.HWServer;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 01.05.16.
 * @author Life4YourGames
 */
public class HWTCPServer {

    private Logger log;

    //-------Begin plain server-------------
    private ServerSocket plainSock;
    private boolean enablePlainTCP;
    private int plainPort;
    //--
    private Thread plainThread;
    private TCPServerRunnable plainRunnable;
    //--------End plain server---------------

    //-------Begin secure server-------------
    private ServerSocket secureSock;
    private boolean enableSecureTCP;
    private int securePort;
    //--
    private Thread secureThread;
    private TCPServerRunnable secureRunnable;
    //--------End secure server---------------

    private HWServer master;

    private ArrayList<HWTCPClientWorker> ccList;

    private boolean stopped = false;

    public HWTCPServer (HWServer hwServer) {

        log = hwServer.getLogger();
        master = hwServer;

        ConfigNode node;

        plainPort = 11900;
        node = master.getConfig().getNode("tcp", "plain", "port");
        if (node.isVirtual()) {
            node.setInt(plainPort);
        } else {
            plainPort = node.optInt(plainPort);
        }

        securePort = plainPort + 1;
        node = master.getConfig().getNode("tcp", "ssl", "port");
        if (node.isVirtual()) {
            node.setInt(securePort);
        } else {
            securePort = node.optInt(securePort);
        }

        node = master.getConfig().getNode("tcp", "plain", "enable");
        if (node.isVirtual()) {
            node.setBoolean(true);
        }
        enablePlainTCP = node.optBoolean(true);

        node = master.getConfig().getNode("tcp", "ssl", "enable");
        if (node.isVirtual()) {
            node.setBoolean(false);
        }
        enableSecureTCP = node.optBoolean(false);
    }

    public boolean setUp() {

        try {

            if (enablePlainTCP) {
                plainSock = new ServerSocket(plainPort);
                plainRunnable = new TCPServerRunnable(this, plainSock);
                plainThread = new Thread(plainRunnable);
            }

            if (enableSecureTCP) {
                Optional<SSLServerSocketFactory> ssf = master.getSecureSocketFactory();
                if (!ssf.isPresent()) {
                    enableSecureTCP = false;
                    sendLog(this, Level.WARNING, "Unable to initialize SecureTCP: Cannot get ServerSocketFactory");
                } else {
                    secureSock = ssf.get().createServerSocket(securePort);
                    secureRunnable = new TCPServerRunnable(this, secureSock);
                    secureThread = new Thread(secureRunnable);
                }
            }

            ccList = new ArrayList<HWTCPClientWorker>();

            return true;

        } catch (IOException ex) {
            log.severe("Unable to create ServerSocket! " + ex.toString());
            return false;
        }

    }

    public void start() {

        if (enablePlainTCP) {
            plainThread.start();
        }

        if (enableSecureTCP) {
            secureThread.start();
        }

    }

    public synchronized void sendLog(Object sender, Level level, String message) {
        if (sender instanceof HWTCPClientHandler) {
            master.onMessage(sender, level, message);
        } else {
            log.log(level, message);
        }
    }

    public synchronized void sendExc(Object sender, Level level, Exception e) {
        master.onException(sender, level, e);
    }

    public void interruptChildren() {

        ccList.forEach(Thread::interrupt);

    }

    public void stopChildren() {

        ccList.forEach(HWTCPClientWorker::closeConnection);

    }

    public void stop() {

        try {

            stopped = true;

            if (enablePlainTCP) {
                plainThread.interrupt();
                plainSock.close();
            }

            if (enableSecureTCP) {
                secureThread.interrupt();
                secureSock.close();
            }

            //interruptChildren();
            stopChildren();

        } catch (IOException ex) {
            log.warning("Unable to close socket: " + ex.toString());
        }

    }

    public synchronized void addClient(HWTCPClientWorker worker) {
        ccList.add(worker);
    }

    public synchronized void reportRunTermination(TCPServerRunnable runnable) {
        if (runnable == null) return;

        if (runnable == secureRunnable || runnable == plainRunnable) {
            sendLog(this, Level.INFO, "Runnable ended: " + runnable.toString());
        } else {
            sendLog(this, Level.INFO, "#TCPRunableTermination reported by non-child runnable ?");
        }

    }

    public int getPlainPort() {
        return plainPort;
    }

    public int getSecPort() { return securePort; }

    public HWServer getMaster() {
        return master;
    }

    public boolean isStopped() { return stopped; }
}
