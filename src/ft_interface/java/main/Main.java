package main;

import network.MessageRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 09.06.16.
 */
public class Main {

    public static void main(String[] args) {

        Logger.getGlobal().setLevel(Level.FINEST);

        for (Handler h : Logger.getGlobal().getHandlers()) {

            h.setLevel(Level.FINEST);

        }

        try {

            InetSocketAddress addr = new InetSocketAddress("localhost", 11902);
            Socket sock = new Socket();
            sock.connect(addr, 4000);
            MessageRunnable runnable = new MessageRunnable(sock);
            Thread msgThread = new Thread(runnable);
            msgThread.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter token");
            String line = reader.readLine();
            String path = null;
            if (line.startsWith("exit")) {
                return;
            } else {
                if (line.contains("_")) {
                    path = line.substring(line.indexOf('_') + 1);
                    line = line.substring(0, line.indexOf('_'));
                }
                sock.getOutputStream().write(line.getBytes(Charset.forName("utf-8")));
                if (path!=null) {
                    File f = new File(path);
                    FileInputStream fIn = new FileInputStream(f);
                    byte[] buffer = new byte[2048];
                    int len = 0;
                    while ((len = fIn.read(buffer)) > -1) {
                        sock.getOutputStream().write(buffer, 0, len);
                    }
                    fIn.close();
                }
            }
            sock.close();

        } catch (Exception ex) {

            ex.printStackTrace();

        }

    }
}
