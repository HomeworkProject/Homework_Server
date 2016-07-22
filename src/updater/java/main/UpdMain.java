package main;

import de.mlessmann.updates.UpdateManager;
import de.mlessmann.util.apparguments.AppArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 04.07.16.
 */
public class UpdMain {

    public static void main(String[] args) throws IOException {

        //System.out.println(Common.getFirstVersion("iaehfubezgwhwieivwi v4.5.0.1.2 fjeiwj"));

        //HTTPUtils.HTTPGetFile("https://s3.amazonaws.com/Minecraft.Download/versions/1.7.10/1.7.10.jar", "C:\\_Share\\example.txt");

        Logger l = Logger.getGlobal();
        l.setLevel(Level.FINEST);

        UpdateManager updater = new UpdateManager();

        ArrayList<AppArgument> arguments = AppArgument.fromArray(args);

        updater.setArgs(arguments);

        updater.setLogger(l);

        updater.run();

        if (updater.isUpdateAvailable()) {

            l.info("An update is available:");
            l.info(updater.getLastResult().get().getVersion() + ": " + updater.getLastResult().get().getInfo());

        }

    }

}
