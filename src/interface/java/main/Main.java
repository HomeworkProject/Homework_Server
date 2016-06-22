package main;

import network.MessageRunnable;
import sun.rmi.log.LogHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
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

            InetSocketAddress addr = new InetSocketAddress("localhost", 11900);

            Socket sock = new Socket();

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
