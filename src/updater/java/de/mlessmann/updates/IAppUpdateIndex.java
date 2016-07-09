package de.mlessmann.updates;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

/**
 * Created by Life4YourGames on 07.07.16.
 */
public interface IAppUpdateIndex {

    String getIdentifier();

    String getType();

    //boolean acceptJSON(JSONObject o);

    //boolean acceptJSON(JSONArray a);

    boolean acceptString(String s);

    Optional<IAppRelease> updateAvailable(String currentVersion, boolean preReleases);

    boolean downloadTo(String version, String cacheDir);

    boolean downloadLatest(boolean preReleases);

}
