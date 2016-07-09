package main;

import java.io.IOException;
import java.util.ArrayList;

import de.mlessmann.hwserver.HWServer;
import de.mlessmann.util.apparguments.AppArgument;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class Main {


    public static void main (String[] args) throws IOException {

        ArrayList<AppArgument> aa = AppArgument.fromArray(args);

        HWServer hwServer = new HWServer();

        hwServer.preInitialize();

        aa.forEach(hwServer::setArg);

        hwServer.initialize();

        hwServer.start();

    }


}
