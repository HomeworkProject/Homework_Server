package de.mlessmann.updates.indices.github;

import de.mlessmann.http.HTTPUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 11.07.16.
 */
public class nativeGitHubAsset {

    private JSONObject json;

    public nativeGitHubAsset(JSONObject o) {

        json = o;

    }

    public int downloadTo(String dir, Logger l) {

        try {

            HTTPUtils.HTTPGetFile(json.getString("browser_download_url"), dir + File.separator + json.getString("name"));

        } catch (MalformedURLException e) {

            e.printStackTrace();
            return 1;

        } catch (IOException e) {

            e.printStackTrace();
            return 1;

        }

        File f = new File(dir + File.separator + json.getString("name"));

        return f.isFile() ? 0 : 1;

    }

}
