package de.mlessmann.updates;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Life4YourGames on 07.07.16.
 */
public interface IAppUpdateIndex {

    String getIdentifier();

    String getType();

    boolean acceptJSON(JSONObject o);

    boolean acceptJSON(JSONArray a);

}
