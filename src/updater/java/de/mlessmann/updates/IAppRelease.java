package de.mlessmann.updates;

import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 09.07.16.
 */
public interface IAppRelease {

    public String getVersion();

    public String getInfo();

    public int downloadTo(String dir, Logger l);

}
