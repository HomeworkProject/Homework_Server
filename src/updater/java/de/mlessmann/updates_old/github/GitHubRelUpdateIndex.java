package de.mlessmann.updates_old.github;

import de.mlessmann.updates_old.HWUpdateIndex;
import de.mlessmann.updates_old.IReleaseInfo;
import de.mlessmann.updates_old.IUpdateIndex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 * Created by Life4YourGames on 06.07.16.
 */

@HWUpdateIndex(JSONType="Array")
public class GitHubRelUpdateIndex implements IUpdateIndex {

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
