package de.mlessmann.updates;

import de.mlessmann.common.annotations.Nullable;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.updates.indices.IIndexType;
import de.mlessmann.updates.indices.IRelease;

import java.util.List;

/**
 * Created by Life4YourGames on 03.10.16.
 */
public class HWUpdate {

    public static final String DEFURL = "http://dev.m-lessmann.de/hwserver/updateConfig.json";
    public static final String DEFREV = null;

    private HWServer server;

    private String confUrl = DEFURL;
    private String confRev = DEFREV;

    private Updater updater;

    public HWUpdate(HWServer server) {
        this.server = server;
        updater = new Updater();
        updater.setLogReceiver(server);
        updater.initProvider();
    }

    // --- --- Setter --- ---

    public void setConfUrl(String url) { this.confUrl = url; }
    public void setConfRev(String rev) { this.confRev = rev; }

    // --- --- Getter --- ---

    public String getConfUrl() { return confUrl; }
    public String getConfRev() { return confRev; }

    // --- --- RUN    --- ---

    public boolean check() {
        updater.setConfURI(confUrl);
        updater.setRevision(confRev);

        if (!updater.retrieveConfig()) {
            return false;
        }
        if (!updater.readConfig()) {
            return false;
        }
        if (!updater.retrieveIndex()) {
            return false;
        }
        if (!updater.readIndex()) {
            return false;
        }
        return true;
    }

    public List<IIndexType> getSucceededTypes() {
        return updater.getSucceededTypes();
    }

    @Nullable
    public IRelease getLatest() {
        return getNewer("0.0.0.0");
    }

    @Nullable
    public IRelease getNewer(String version) {
        List<IIndexType> types = getSucceededTypes();
        if (types.isEmpty())
            return null;
        final IRelease[] res = {null};
        final String[] v = {version};
        types.forEach(t -> {
            t.getReleases().forEach(r -> {
                if (r.compareTo(v[0]) == -1) {
                    v[0] = r.version();
                    res[0] = r;
                }
            });
        });
        return res[0];
    }
}
