package de.mlessmann.updates.github;

import de.mlessmann.updates.HWUpdater;
import de.mlessmann.updates.IReleaseInfo;
import de.mlessmann.util.Common;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Life4YourGames on 05.07.16.
 */
public class GitHubReleaseInfo implements IReleaseInfo {

    private JSONObject json = new JSONObject();
    private ArrayList<GitHubAsset> assets = new ArrayList<GitHubAsset>();

    private String version = "0.0.0.0";

    @Override
    public void setJSON(JSONObject o) {

        json = o;

        assets = new ArrayList<GitHubAsset>();

        if (json.has("assets")) {

            JSONArray a = json.getJSONArray("assets");

            for (Object obj : a) {

                assets.add(new GitHubAsset((JSONObject) obj));

            }

        }

        String s = json.optString("tag_name", "v0.0.0.0");

        version = Common.getFirstVersion(s);

    }

    @Override
    public boolean isPreRelease() {

        return json.optBoolean("prerelease", true);

    }

    @Override
    public String getVersionNum() {

        return version;

    }

    @Override
    public String getBranch() {

        return json.optString("target_commitish", "default");

    }

    @Override
    public String getName() {

        return json.optString("tag_name", "default");

    }

    @Override
    public void downloadTo(HWUpdater u, String cacheDir) {

        File cache = new File(cacheDir);

        if (!cache.isFile()) {

            u.getLogger().severe("Unable to download update: Cache is a file (" + cacheDir + ")");
            return;

        }

        if (!cache.exists() && !cache.mkdirs()) {

            u.getLogger().severe("Unable to create cache: " + cacheDir);
            return;

        }

        int i = 1;

        for (GitHubAsset a : assets) {

            try {

                a.downloadTo(cacheDir + File.pathSeparator + a.getName());
                i++;

            } catch (IOException ex) {

                u.getLogger().severe("Unable to download asset no " + i + ": "+ a.getName() + ": " + ex.toString());
                return;

            }

        }

        u.getLogger().info("Downloaded " + i + " assets");

    }

}
