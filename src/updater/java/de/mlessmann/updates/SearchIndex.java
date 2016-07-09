package de.mlessmann.updates;

import de.mlessmann.http.HTTPUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import de.mlessmann.util.ObjectBox;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 06.07.16.
 */
public class SearchIndex {

    public static ObjectBox<SearchIndex> fromUrl(String branch, String url, Logger l) {

        l.finest("Retrieving seachIndex from: " + url);

        SearchIndex i = null;

        String content;

        try {

            if (url.startsWith("https")) {

                content = HTTPUtils.GETHTTPSText(url, 0, null);

            } else {

                content = HTTPUtils.GETHTTPText(url, 0, null);

            }

        } catch (MalformedURLException ex) {

            l.warning("SearchIndex: Invalid URL \"" + url + "\"");
            return new ObjectBox<SearchIndex>(null, 2);

        } catch (IOException ex) {

            l.warning("SearchIndex: Unable to check for updates: " + ex.toString());
            //ex.printStackTrace();
            return new ObjectBox<SearchIndex>(null, 1);

        }

        if (content == null) {

            return new ObjectBox<SearchIndex>(null, 3);

        }


        String c = content;

        try {

            //SearchIndex

            i = new SearchIndex(new JSONObject(c).getJSONObject(branch));

        } catch (Exception e) {

            l.warning("SearchIndex: Unable to initialize: " + e.toString());
            return new ObjectBox<SearchIndex>(null, 4);

        }

        return new ObjectBox<SearchIndex>(i, 0);

    }

    private JSONObject obj;

    public SearchIndex(JSONObject o) {

        obj = o;

    }

    public String getUpdIndexUrl() {

        return obj.optString("url", "");

    }

    public String getUpdIndexType() {

        return obj.getString("type");

    }

    public JSONObject getJSON() { return obj; }

}
