package main;

import java.io.IOException;
import de.mlessmann.hwserver.HWServer;

/**
 * Created by Life4YourGames on 29.04.16.
 */
public class Main {


    public static void main(String[] args) throws IOException {

        HWServer hwServer = new HWServer();

        hwServer.setArgs(args);

    }


}
