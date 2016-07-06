package de.mlessmann.updates;

import org.json.JSONObject;

/**
 * Created by Life4YourGames on 05.07.16.
 */
public interface IReleaseInfo {

    void setJSON(JSONObject o);

    boolean isPreRelease();

    String getVersionNum();

    String getBranch();

    String getName();

    void downloadTo(HWUpdater u, String cacheDir);

}
