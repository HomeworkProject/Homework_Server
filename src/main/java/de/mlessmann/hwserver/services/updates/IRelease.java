package de.mlessmann.hwserver.services.updates;

import java.util.Map;

/**
 * Created by Life4YourGames on 17.09.16.
 */
public interface IRelease {

    String getVersion();
    String getBranch();

    boolean isDraft();
    boolean isPreRelease();

    int compareTo(String v2);

    Map<String, String> getFiles();
}
