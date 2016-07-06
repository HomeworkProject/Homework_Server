package main;

import de.mlessmann.http.HTTPUtils;
import de.mlessmann.updates.HWUpdater;
import de.mlessmann.util.Common;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 04.07.16.
 */
public class UpdMain {

    public static void main(String[] args) throws IOException {

        //System.out.println(Common.getFirstVersion("iaehfubezgwhwieivwi v4.5.0.1.2 fjeiwj"));

        //HTTPUtils.HTTPGetFile("https://s3.amazonaws.com/Minecraft.Download/versions/1.7.10/1.7.10.jar", "C:\\_Share\\example.txt");

        HWUpdater updater = new HWUpdater();

        updater.setLogger(Logger.getLogger("Test"));

        updater.run();

    }

}
