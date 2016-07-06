package de.mlessmann.updates;

import de.mlessmann.updates.github.GitHubReleaseInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 * Created by Life4YourGames on 05.07.16.
 */
public class UpdateIndex {

    private JSONArray json;
    private HashMap<String, IReleaseInfo> releases;

    public void setJson(JSONArray j) {

        json = j;

        releases = new HashMap<String, IReleaseInfo>();

        for (Object o : json) {

            IReleaseInfo i = new GitHubReleaseInfo();

            i.setJSON((JSONObject) o);

            releases.put(i.getName(), i);

        }

    }

    public ArrayList<IReleaseInfo> getReleases() {

        ArrayList<IReleaseInfo> a = new ArrayList<IReleaseInfo>();

        releases.forEach((k, i) -> a.add(i));

        return a;

    }

    public Optional<IReleaseInfo> getRelease(String releaseName) {

        IReleaseInfo i = null;

        if (releases.containsValue(releaseName)) {

            i = releases.get(releaseName);

        }

        return Optional.ofNullable(i);

    }

}
