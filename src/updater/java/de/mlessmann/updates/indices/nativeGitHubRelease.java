package de.mlessmann.updates.indices;

import de.mlessmann.updates.IAppRelease;
import de.mlessmann.util.Common;
import org.json.JSONArray;
import org.json.JSONObject;

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

}
