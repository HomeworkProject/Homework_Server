package de.mlessmann.updates_old;

import org.json.JSONObject;

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

    /*
    Override seachIndex!
     */
    private String directUrl = null;
    private String directType = null;

    private int mode = 1;
    private Logger l = Logger.getGlobal();

    public void run() {

        switch (mode) {
            case 1: checkForUpdate(); break;
            default: l.info("Updater: Unknown mode: " + mode); break;
        }

    }

    public HWUpdater() {

        if (main == null) {

            main = this;

        }

    }

    private void checkForUpdate() {

        updateAble = false;

        SearchIndex index;

        if (directUrl == null) {

            Optional<SearchIndex> i = SearchIndex.fromUrl(url, l);

            if (!i.isPresent()) return;

            index = i.get();

        } else {

            if (directType == null) {

                l.severe("SearchIndex cannot be overridden without \"url-type\" being set!");
                return;

            }

            index = new SearchIndex(
                        new JSONObject()
                                .put("url", directUrl)
            );

        }

        String updateUrl = index.getUpdIndexUrl();

        /*

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

        */

    }

    public boolean hasUpdate() { return updateAble; }

    public Logger getLogger() { return l; }

    public void setLogger(Logger lo) { l = lo; }

    public void setUrl(String u) { url = u; }

    public void setDirectUrl(String u) { directUrl = u; }

    public void setDirectType(String t) { directType = t; }

    public void setMode(int m) { mode = m; }

}
