package de.mlessmann.network;

import de.mlessmann.hwserver.HWServer;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

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

        plainPort = master.getConfig().getJSON().optInt("plain_tcp_port", 11900);
        securePort = master.getConfig().getJSON().optInt("secure_tcp_port", plainPort + 1);

        enablePlainTCP = master.getConfig().getJSON().optBoolean("plain_tcp_enable", true);
        enableSecureTCP = master.getConfig().getJSON().optBoolean("secure_tcp_enable", false);

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
