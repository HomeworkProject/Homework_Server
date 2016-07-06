package de.mlessmann.updates;

import de.mlessmann.http.HTTPUtils;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.util.Common;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 04.07.16.
 */
public class HWUpdater implements Runnable {

    public static String indexTree = "early-access";

    public static HWUpdater getMain() {

        if (main == null) new HWUpdater();

        return main;

    }

    public static HWUpdater main = null;

    //----------------------------------

    private boolean updateAble = false;
    private String url = "http://schule.m-lessmann.de/hwserver/updates.json";
    private int mode = 1;
    private Logger l = Logger.getGlobal();

    public void run() {

        switch (mode) {
            case 1: checkForUpdate(); break;
        }

    }

    public HWUpdater() {

        if (main == null) {

            main = this;

        }

    }

    private void checkForUpdate() {

        Optional<String> content;

        try {

            if (url.startsWith("https")) {

                content = HTTPUtils.GETHTTPSText(url, 0, null);

            } else {

                content = HTTPUtils.GETHTTPText(url, 0, null);

            }

        } catch (MalformedURLException ex) {

            l.warning("Updater: Invalid URL \"" + url + "\"");
            updateAble = false;
            return;

        } catch (IOException ex) {

            l.warning("Updater: Unable to check for updates: " + ex.toString());
            ex.printStackTrace();
            updateAble = false;
            return;

        }

        if (!content.isPresent()) {

            updateAble = false;
            return;

        }


        String c = content.get();

        try {

            //SearchIndex

            JSONObject obj = new JSONObject(c);

            JSONObject updateIndex = obj.getJSONObject(indexTree);

            boolean allowPreRel = updateIndex.getBoolean("prereleases");
            String updateUrl = updateIndex.getString("url");

            //Retrieve UpdateIndex

            try {

                if (updateUrl.startsWith("https")) {

                    content = HTTPUtils.GETHTTPSText(updateUrl, 0, null);

                } else {

                    content = HTTPUtils.GETHTTPText(updateUrl, 0, null);

                }

            } catch (MalformedURLException ex) {

                l.severe("Updater: Malformed URL in searchIndex: \"" + updateUrl + "\"");
                updateAble = false;
                return;

            } catch (IOException ex) {

                l.severe("Updater: Unable to check for updates in updateIndex: " + ex.toString());
                updateAble = false;
                return;

            }

            if (!content.isPresent()) {

                updateAble = false;
                return;

            }

            String sUpdate = content.get();

            JSONArray updateObject = new JSONArray(sUpdate);

            //UpdateIndex

            UpdateIndex index = new UpdateIndex();

            index.setJson(updateObject);

            ArrayList<IReleaseInfo> releases = index.getReleases();

            if (releases.size() == 0) {

                l.info("No releases found");
                updateAble = false;
                return;

            }

            IReleaseInfo i = releases.get(releases.size() - 1);

            l.finer("Checking " + HWServer.VERSION + " against remote " + i.getVersionNum());

            if (Common.compareVersions(i.getVersionNum(), HWServer.VERSION) == -1) {

                l.info("Update found: Version \"" + i.getVersionNum() + "\" available");
                updateAble = true;

            }

        } catch (Exception e) {

            l.severe("Updater: Exception occured while trying to check for updates: " + e.toString());
            e.printStackTrace();
            updateAble = false;
            return;

        }

    }

    public boolean hasUpdate() { return updateAble; }

    public Logger getLogger() { return l; }

    public void setLogger(Logger lo) { l = lo; }

}
