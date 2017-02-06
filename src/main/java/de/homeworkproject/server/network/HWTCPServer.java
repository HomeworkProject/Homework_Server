package de.homeworkproject.server.network;

import de.homeworkproject.server.hwserver.HWServer;
import de.homeworkproject.server.network.filetransfer.FileTransferServer;
import de.mlessmann.config.ConfigNode;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private String cipherProtocol;
    private ServerSocket secureSock;
    private boolean enableSecureTCP;
    private int securePort;
    //--
    private Thread secureThread;
    private TCPServerRunnable secureRunnable;
    //--------End secure server---------------

    //-------Begin file transfer server-------------
    private ServerSocket ftSock;
    private boolean enableFT;
    private int ftPort;
    //--
    private Thread ftThread;
    private FileTransferServer ftRunnable;
    //--------End file transfer server---------------

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

        ftPort = plainPort + 2;
        node = master.getConfig().getNode("tcp", "ft", "port");
        if (node.isVirtual()) {
            node.setInt(ftPort);
        } else {
            ftPort = node.optInt(ftPort);
        }

        //// --- --- --- --- ////

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

        node = master.getConfig().getNode("tcp", "ft", "enable");
        if (node.isVirtual()) {
            node.setBoolean(false);
        }
        enableFT = node.optBoolean(false);

        //// --- --- --- --- ////

        node = master.getConfig().getNode("tcp", "ssl", "protocol");
        if (node.isVirtual()) {
            node.setString("default");
        }
        cipherProtocol = node.optString("default");
    }

    public boolean setUp() {
        try {
            if (enablePlainTCP) {
                plainSock = new ServerSocket(plainPort);
                plainRunnable = new TCPServerRunnable(this, plainSock, "Plain");
                plainThread = new Thread(plainRunnable);
            }
            if (enableSecureTCP) {
                setUpSSL();
            }
            if (enableFT) {
                ftSock = new ServerSocket(ftPort);
            }
            ftRunnable = new FileTransferServer(ftSock, 16, master);
            ftThread = new Thread(ftRunnable);

            ccList = new ArrayList<HWTCPClientWorker>();
            return true;
        } catch (IOException ex) {
            log.severe("Unable to create ServerSocket! " + ex.toString());
            return false;
        }
    }

    private void setUpSSL() {
        try {
            if (System.getProperty("javax.net.ssl.keyStore", null) == null) {
                System.setProperty("javax.net.ssl.keyStore",
                        master.getConfig().getNode("tcp", "ssl", "keystore").optString("keystore.ks"));
                if (master.getConfig().getNode("tcp", "ssl", "keystore").isVirtual())
                    master.getConfig().getNode("tcp", "ssl", "keystore").setString("keystore.ks");
            }
            if (System.getProperty("javax.net.ssl.keyStorePassword", null) == null) {
                System.setProperty("javax.net.ssl.keyStorePassword",
                        master.getConfig().getNode("tcp", "ssl", "password").optString("password"));
                if (master.getConfig().getNode("tcp", "ssl", "password").isVirtual())
                    master.getConfig().getNode("tcp", "ssl", "password").setString("MyKeystorePass");
            }

            SSLContext sslCtx = SSLContext.getInstance(cipherProtocol);
            if (sslCtx.getDefaultSSLParameters().getCipherSuites().length < 1) {
                sendLog(this, Level.WARNING, "SSLCtx for " + cipherProtocol + " does NOT support any ciphers!");
            }
            Arrays.stream(sslCtx.getDefaultSSLParameters().getCipherSuites()).forEach(
                    c -> sendLog(null, Level.FINEST, "Cipher: " + c)
            );
            SSLServerSocketFactory ssf = sslCtx.getServerSocketFactory();
            secureSock = ssf.createServerSocket(securePort);
            Arrays.stream(((SSLServerSocket) secureSock).getEnabledCipherSuites()).forEach(
                    c -> sendLog(null, Level.FINEST, "Enabled Cipher: " + c)
            );
            secureRunnable = new TCPServerRunnable(this, secureSock, "SSL");
            secureThread = new Thread(secureRunnable);
        } catch (NoSuchAlgorithmException e) {
            sendLog(this, Level.SEVERE, "Unable to initialize SecureTCP: NSAException:");
            sendExc(this, Level.SEVERE, e);
            enableSecureTCP = false;
        } catch (IOException e) {
            sendLog(this, Level.SEVERE, "Unable to create SecServerSock: IOE");
            sendExc(this, Level.SEVERE, e);
            enableSecureTCP = false;
        }
    }

    public void start() {
        if (enablePlainTCP) {
            plainThread.start();
        }
        if (enableSecureTCP) {
            secureThread.start();
        }
        if (enableFT) {
            ftThread.start();
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
            ftRunnable.close();
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

    public int getFtPort() { return ftPort; }

    public FileTransferServer getFTManager() { return ftRunnable; }

    public HWServer getMaster() {
        return master;
    }

    public boolean isStopped() { return stopped; }
}
