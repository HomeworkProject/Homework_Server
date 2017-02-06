package main;

import de.mlessmann.common.apparguments.AppArgument;
import network.MessageRunnable;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 09.06.16.
 */
public class Main {

    public static void main(String[] args) {
        Map<String, AppArgument> argMap = new HashMap<String, AppArgument>();
        AppArgument.fromArray(args).forEach(a -> {
            argMap.put(a.getKey(), a);
        });


        Logger.getGlobal().setLevel(Level.FINEST);
        for (Handler h : Logger.getGlobal().getHandlers()) {
            h.setLevel(Level.FINEST);
        }
        System.setProperty("javax.net.ssl.trustStore", "keystore.ks");
        System.setProperty("javax.net.ssl.trustStorePassword", "hwserver");


        try {
            String hName = argMap.containsKey("--host") ? argMap.get("--host").getValue() : "localhost";
            Integer port = argMap.containsKey("--port") ? Integer.parseInt(argMap.get("--port").getValue()) : 11900;
            InetSocketAddress addr = new InetSocketAddress(hName, port);

            Socket sock = null;
            if (argMap.containsKey("--ssl")) {
                System.out.println("Using SSL to connect");
                SSLSocketFactory sf = ((SSLSocketFactory) SSLSocketFactory.getDefault());
                sock = sf.createSocket();
            } else {
                sock = new Socket();
            }
            System.out.println("Connecting to: " + addr.toString());
            sock.connect(addr, 4000);
            MessageRunnable runnable = new MessageRunnable(sock);
            Thread msgThread = new Thread(runnable);
            msgThread.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String line = reader.readLine();

                if (line.startsWith("exit")) {
                    break;
                } else {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                    writer.write(line + "\n");
                    writer.flush();
                }
            }
            sock.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
