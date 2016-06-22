package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 09.06.16.
 */
public class MessageRunnable implements Runnable {


    private Socket sock;

    public MessageRunnable(Socket mySock) {

        sock = mySock;

    }

    public void run() {

        Logger.getGlobal().severe("Running message handler");

        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            while (true) {

                String line = reader.readLine();

                Logger.getGlobal().severe(line);

                if (!sock.isConnected()) {

                    Logger.getGlobal().severe("disconnected");

                    break;

                }

            }

        } catch (IOException ex) {

            Logger.getGlobal().severe(ex.toString());

        }

    }

}
