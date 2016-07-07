package de.mlessmann.updates_old.github;

import de.mlessmann.http.HTTPUtils;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by Life4YourGames on 05.07.16.
 */
public class GitHubAsset {

    private JSONObject json;

    public GitHubAsset(JSONObject o) {

        json = o;

    }

    public String getUrl() {

        return json.getString("browser_download_url");

    }

    public String getName() {

        return json.getString("name");

    }

    public void downloadTo(String targetPath) throws IOException {

        HTTPUtils.HTTPGetFile(getUrl(), targetPath);

    }

}
