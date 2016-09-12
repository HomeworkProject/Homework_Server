package de.mlessmann.hwserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 02.09.16.
 */
public class CommandLine {

    private HWServer server;
    private final Logger LOG;

    public CommandLine(HWServer server) {
        this.server = server;
        LOG = server.getLogger();
    }

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        loop: while (true) {

            try {

                String command = reader.readLine();

                switch (command) {
                    case "stop":
                        server.stop();
                        break;
                    case "quit": ;
                    case "exit":
                        server.stop();
                        break loop;
                        //break;
                    case "reload":
                        server.stop().start();
                        break;
                    case "start":
                        server.start();
                        break;
                    case "update":
                        server.checkForUpdate();
                        break;
                    default: System.out.println("Unknown command: " + command); break;
                }

            } catch (IOException ex) {
                LOG.throwing(this.getClass().toString(), "start", ex);
            }
        }
    }

}
