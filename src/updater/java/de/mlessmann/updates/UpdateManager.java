package de.mlessmann.updates;

import de.mlessmann.http.HTTPUtils;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.reflections.AppIndexLoader;
import de.mlessmann.reflections.UpdIndexProvider;
import de.mlessmann.util.ObjectBox;
import de.mlessmann.util.apparguments.AppArgument;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 07.07.16.
 */
public class UpdateManager implements Runnable {

    public static final int M_TESTONLY = 0;
    public static final int M_CHECKONLY = 1;
    public static final int M_CHECKANDUPDATE = 2;
    public static final int M_FORCEUPDATE = 3;

    private HashMap<String, String> options = new HashMap<String, String>();

    private String searchUrl = "http://schule.m-lessmann.de/hwserver/updates.json";
    private String searchBranch = "early-access";
    private JSONObject searchContent;

    private SearchIndex index;

    private String directUrl = null;
    private String directType = null;

    private UpdIndexProvider myIndexProvider;

    private Optional<IAppRelease> lastResult = Optional.empty();
    private boolean updateAvailable = false;
    private boolean preReleases = false;

    private Logger l = Logger.getGlobal();

    private int mode = 0;
    private int errorCode = 0;

    public int getErrorCode() { return errorCode; }

    public void setSearchUrl(String u) { searchUrl = u; }

    public void setDirectUrl(String u) { directUrl = u; }

    public void setDirectType(String t) {directType = t; }

    public void setMode(int m) { mode = m; }

    public void run() {

        if (myIndexProvider == null) {

            myIndexProvider = new UpdIndexProvider();
            myIndexProvider.setLogger(l);

            AppIndexLoader loader = new AppIndexLoader();

            loader.setLogger(l);
            loader.setMaster(this);
            loader.setProvider(myIndexProvider);

            l.info("Trying to load UpdIndex types");
            loader.loadAll();

        }

        switch (mode) {

            case 1: errorCode = checkForUpdate(); break;
            default: l.severe("Unsupported update mode: " + mode);

        }


    }

    private boolean getSearchIndex() {

        if (index != null) return true;

        if (directUrl != null && directType != null) {

            searchContent = new JSONObject()
                    .put("url", directUrl)
                    .put("type", directType);

            index = new SearchIndex(searchContent);

            return true;

        }

        ObjectBox<SearchIndex> i = SearchIndex.fromUrl(searchBranch, searchUrl, l);

        if (i.isPresent()) {

            index = i.get();

            return true;

        }

        l.warning("Unable to retrieve SearchIndex!");

        return false;

    }

    private int checkForUpdate() {

        updateAvailable = false;

        if (!getSearchIndex()) {

            return 1;

        }

        String url = index.getUpdIndexUrl();
        String type = index.getUpdIndexType();

        ArrayList<IAppUpdateIndex> indices = myIndexProvider.getByType(type);

        if (indices.size() == 0) {

            l.warning("Unknown index type: " + type);
            return 1;

        } else if (indices.size() > 1) {

            l.info("Multiple indices for type: " + type);
            l.info("Using: " + indices.get(0).getIdentifier());

        }

        l.info("Sending request to update url");

        String content;

        try {

            if (url.startsWith("https")) {

                content = HTTPUtils.GETHTTPSText(url, 0, null);

            } else {

                content = HTTPUtils.GETHTTPText(url, 0, null);

            }

        } catch (MalformedURLException ex) {

            l.warning("UpdIndex: Invalid URL \"" + url + "\"");
            return 1;
        } catch (IOException ex) {

            l.warning("SearchIndex: Unable to check for updates: " + ex.toString());
            //ex.printStackTrace();
            return 1;
        }

        if (content == null) {

            return 1;

        }


        String c = content;


        int i = 0;
        IAppUpdateIndex index = null;

        while (i < indices.size()) {
            index = indices.get(i++);

            if (!index.acceptString(c)) {

                l.warning("Index " + index.getIdentifier() + " returned false on #acceptString...");


            } else { break; }

        }

        if (index == null) return 1;


        lastResult = index.updateAvailable(HWServer.VERSION, preReleases);
        updateAvailable = lastResult.isPresent();

        return 0;

    }

    private void getUpdate() {

    }

    public void setLogger(Logger log) { l = log; }

    public Logger getLogger() { return l; }

    public boolean isUpdateAvailable() { return updateAvailable; }

    public void reset() {

        index = null;
        updateAvailable = false;

    }

    public void fullReset() {

        reset();
        searchUrl = "http://schule.m-lessmann.de/hwserver/updates.json";
        directUrl = null;
        directType = null;

    }

    public UpdateManager allowPreReleases() { preReleases = true; return this; }

    public UpdateManager disallowPreReleases() { preReleases = false; return this; }

    public HashMap<String, String> getOptions() { return options; }

    public Optional<IAppRelease> getLastResult() { return lastResult; }

    //------------------ Accept start arguments -----------------------

    public void setArgs(ArrayList<AppArgument> aa) {

        for (AppArgument a : aa) {

            switch (a.getKey()) {

                case "--search-url": setSearchUrl(a.getValue()); break;
                case "--url-type": setDirectType(a.getValue()); break;
                case "--url": setDirectUrl(a.getValue()); break;
                case "--check-only": setMode(1); break;
                case "--o":
                    String v = a.getValue();
                    int diff = v.indexOf(",");
                        getOptions().put(
                            v.substring(0, diff),
                            v.substring(diff + 1, v.length() - 1)
                    );
                default: l.warning("Updater: Unknown argument \"" + a.getKey() + "\"");
            }

        }

    }

}
