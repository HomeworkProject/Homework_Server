package main;

import java.io.*;
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
            sock.setSoTimeout(10000);
            //MessageRunnable runnable = new MessageRunnable(sock);
            //Thread msgThread = new Thread(runnable);
            //msgThread.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.println("Enter token");
                String line = reader.readLine();
                String path = null;
                if (line.startsWith("exit")) {
                    break;
                } else {
                    if (line.contains("_")) {
                        path = line.substring(line.indexOf("_") + 1);
                        line = line.substring(0, line.indexOf("_"));
                    }
                    System.out.println("Sending token " + line);
                    OutputStream sockOut = sock.getOutputStream();
                    InputStream sockIn = sock.getInputStream();
                    sockOut.write(line.getBytes(Charset.forName("utf-8")));
                    sockOut.flush();
                    Thread.sleep(5000);
                    if (path != null) {
                        File f = new File(path);
                        System.out.println("Sending file: " + f.getAbsolutePath());
                        FileInputStream fIn = new FileInputStream(f);
                        byte[] buffer = new byte[1024];
                        int len = 0;
                        while ((len = fIn.read(buffer)) > -1) {
                            sockOut.write(buffer, 0, len);
                            System.out.println("Writing:" + new String(buffer));
                            sockOut.flush();
                        }
                    } else {
                        System.out.println("Receiving to \"ft.out\"");
                        try (FileOutputStream fOut = new FileOutputStream(new File("ft.out"))) {
                            byte[] buffer = new byte[1024];
                            int len = 0;
                            int total = 0;
                            while ((len = sockIn.read(buffer)) > -1) {
                                fOut.write(buffer, 0, len);
                                total += len;
                                System.out.println("Writing... [" + total + "]");
                                fOut.flush();
                            }
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            sock.close();

        } catch (Exception ex) {

            ex.printStackTrace();

        }

    }
}
