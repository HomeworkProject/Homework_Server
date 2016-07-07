package de.mlessmann.updates_old;

import de.mlessmann.http.HTTPUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 05.07.16.
 */
public class UpdateIndexProvider {

    public static Optional<IUpdateIndex> fromUrl(String type, String url, Logger l) {

        IUpdateIndex i = null;

        Optional<String> content;

        try {

            if (url.startsWith("https")) {

                content = HTTPUtils.GETHTTPSText(url, 0, null);

            } else {

                content = HTTPUtils.GETHTTPText(url, 0, null);

            }

        } catch (MalformedURLException ex) {

            l.warning("UpdateIndex: Invalid URL \"" + url + "\"");
            return Optional.empty();

        } catch (IOException ex) {

            l.warning("UpdateIndex: Unable to check for updates: " + ex.toString());
            //ex.printStackTrace();
            return Optional.empty();

        }

        if (!content.isPresent()) {

            return Optional.empty();

        }


        String c = content.get();

        //TODO: Determine index type and instantiate correct one

        /*

        try {

            switch (type) {

                case: "github-releases": i = f

            }
            //SearchIndex

            i = new UpdateIndex();

            i.setJson(new JSONArray(c));

        } catch (Exception e) {

            l.warning("SearchIndex: Unable to initialize: " + e.toString());

        }

        */

        return Optional.ofNullable(i);

    }

}
