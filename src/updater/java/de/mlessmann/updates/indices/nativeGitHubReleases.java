package de.mlessmann.updates.indices;

import de.mlessmann.reflections.AppUpdateIndex;
import de.mlessmann.updates.IAppRelease;
import de.mlessmann.updates.IAppUpdateIndex;
import de.mlessmann.updates.UpdateManager;
import de.mlessmann.util.Common;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Created by Life4YourGames on 07.07.16.
 */
@AppUpdateIndex( JSONType = "Array")
public class nativeGitHubReleases implements IAppUpdateIndex {

    private String TYPE = "github-releases";
    private String ID = "de.mlessmann.updater.github-releases";

    private ArrayList<nativeGitHubRelease> releases = new ArrayList<nativeGitHubRelease>();

    private UpdateManager myMaster;

    private nativeGitHubRelease latest;

    public nativeGitHubReleases(UpdateManager master) {

        myMaster = master;

    }


    @Override
    public String getType() { return TYPE; }

    @Override
    public String getIdentifier() { return ID; }

    //@Override
    //public boolean acceptJSON(JSONArray a) { return false; }

    //@Override
    //public boolean acceptJSON(JSONObject o) { return false; }

    @Override
    public boolean acceptString(String s) {

        try {

            JSONArray remInfo = new JSONArray(s);

            for (Object o : remInfo) {

                nativeGitHubRelease r = new nativeGitHubRelease((JSONObject) o);

                releases.add(r);

            }

        } catch (JSONException ex) {

            ex.printStackTrace();

            return false;

        }

        return true;

    }

    @Override
    public Optional<IAppRelease> updateAvailable(String currentVersion, boolean preReleases) {

        String selBranch = myMaster.getOptions().getOrDefault("branch", null);

        for (nativeGitHubRelease r : releases) {

            if (selBranch != null) {

                //Ignore wrong branches
                if (!r.getBranch().equals(selBranch)) continue;

            }
            if (r.isPreRelease() && !preReleases) continue;

            //Ignore drafts
            if (r.isDraft()) continue;

            if (Common.compareVersions(r.getVersion(), currentVersion) == - 1) {

                latest = r;

                return Optional.of(r);

            }

        }

        return Optional.empty();

    }

    @Override
    public boolean downloadLatest(boolean preReleases) {
        return false;
    }

    @Override
    public boolean downloadTo(String version, String cacheDir) {
        return false;
    }

}
