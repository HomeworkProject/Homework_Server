package main;

import de.mlessmann.updates_old.HWUpdater;
import de.mlessmann.util.apparguments.AppArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 04.07.16.
 */
public class UpdMain {

    public static void main(String[] args) throws IOException {

        //System.out.println(Common.getFirstVersion("iaehfubezgwhwieivwi v4.5.0.1.2 fjeiwj"));

        //HTTPUtils.HTTPGetFile("https://s3.amazonaws.com/Minecraft.Download/versions/1.7.10/1.7.10.jar", "C:\\_Share\\example.txt");

        Logger l = Logger.getLogger("Test");

        HWUpdater updater = new HWUpdater();

        ArrayList<AppArgument> arguments = AppArgument.fromArray(args);

        for (AppArgument a : arguments) {

            switch (a.getKey()) {

                case "--search-url": updater.setUrl(a.getValue()); break;
                case "--url-type": updater.setDirectType(a.getValue()); break;
                case "--url": updater.setUrl(a.getValue()); break;
                case "--check-only": updater.setMode(1); break;
                default: l.warning("Unknown argument \"" + a.getKey() + "\"");
            }

        }

        updater.setLogger(l);

        updater.run();

    }

}
