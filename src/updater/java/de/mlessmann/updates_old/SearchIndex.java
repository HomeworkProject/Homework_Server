package de.mlessmann.updates_old;

import de.mlessmann.http.HTTPUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 06.07.16.
 */
public class SearchIndex {

    public static Optional<SearchIndex> fromUrl(String url, Logger l) {

        SearchIndex i = null;

        Optional<String> content;

        try {

            if (url.startsWith("https")) {

                content = HTTPUtils.GETHTTPSText(url, 0, null);

            } else {

                content = HTTPUtils.GETHTTPText(url, 0, null);

            }

        } catch (MalformedURLException ex) {

            l.warning("Updater: Invalid URL \"" + url + "\"");
            return Optional.empty();

        } catch (IOException ex) {

            l.warning("Updater: Unable to check for updates: " + ex.toString());
            //ex.printStackTrace();
            return Optional.empty();

        }

        if (!content.isPresent()) {

            return Optional.empty();

        }


        String c = content.get();

        try {

            //SearchIndex

            i = new SearchIndex(new JSONObject(c));

        } catch (Exception e) {

            l.warning("SearchIndex: Unable to initialize: " + e.toString());

        }

        return Optional.ofNullable(i);

    }

    private JSONObject obj;

    public SearchIndex(JSONObject o) {

        obj = o;

    }

    public String getUpdIndexUrl() {

        return obj.optString("url", "");

    }

    public JSONObject getJSON() { return obj; }

}
