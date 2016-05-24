package main;

import java.io.IOException;
import de.mlessmann.hwserver.HWServer;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class Main {


    public static void main (String[] args) throws IOException {

        //try {

            HWServer hwServer = new HWServer();

            hwServer.preInitialize();

            hwServer.setArgs(args);

            hwServer.initialize();

            hwServer.start();

        /*} catch (Exception ex) {

            System.out.print(ex.toString());
            ex.printStackTrace(System.out);

        }*/


    }


}
