package de.mlessmann.updates.indices.github;

import de.mlessmann.updates.IAppRelease;
import de.mlessmann.util.Common;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 09.07.16.
 */
public class nativeGitHubRelease implements IAppRelease {

    private JSONArray assets = new JSONArray();
    private String version;
    private String branch;
    private boolean preRel = false;
    private boolean draft = false;

    public nativeGitHubRelease(JSONObject json) {

        assets = json.getJSONArray("assets");

        version = Common.getFirstVersion(json.getString("tag_name"));

        branch = json.getString("target_commitish");

        preRel = json.getBoolean("prerelease");

        draft = json.getBoolean("draft");

    }

    @Override
    public String getVersion() { return version; }

    @Override
    public String getInfo() { return "Branch: " + getBranch(); }

    public String getBranch() { return branch; }

    public boolean isPreRelease() { return preRel; }

    public boolean isDraft() { return draft; }

    @Override
    public int downloadTo(String dir, Logger l) {

        File f = new File(dir);

        if (f.isFile()) {

            l.severe("Unable to download update: Cache is occupied by file: " + f.getPath());
            return 1;

        }

        if (!f.isDirectory() && !f.mkdirs()) {

            l.severe("Unable to download update: Unable to create cache dir: " + f.getPath());
            return 1;

        }

        int failures = 0;

        for (Object o : assets) {

            JSONObject j = (JSONObject) o;

            nativeGitHubAsset a = new nativeGitHubAsset(j);

            if (a.downloadTo(dir, l) != 0) {

                l.severe("There has been an error while downloading asset: " + j.getString("name"));
                failures++;

            }

        }

        return failures == 0 ? 0 : 1;
    }
}
