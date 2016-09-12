package de.mlessmann.hwserver.services.updates;

import de.mlessmann.common.HTTP;
import de.mlessmann.common.annotations.Nullable;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.updating.indices.IIndexType;
import de.mlessmann.updating.indices.IRelease;
import de.mlessmann.updating.logging.ILogReceiver;
import de.mlessmann.updating.updater.Updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.logging.Level.*;

/**
 * Created by Life4YourGames on 10.09.16.
 */
public class UpdateSvc implements Runnable, ILogReceiver {

    private HWServer server;

    private Thread myThread;

    private Updater updater;

    //Svc configuration
    private boolean selfCheck;
    private boolean autoGetSelfUpdate;

    private boolean check;
    private boolean autoGetUpdate;

    //Results
    private IRelease selfUpdate;

    private String branch;
    private boolean drafts = false;
    private boolean preReleases = false;
    private IRelease update;
    private boolean upgrade;

    //Callbacks
    private List<IUpdateSvcReceiver> listeners;

    public UpdateSvc(HWServer server) {
        this.server = server;
        updater = new Updater(this, null);
        listeners = new ArrayList<IUpdateSvcReceiver>();
    }

    public boolean isBusy() { return myThread != null && myThread.isAlive(); }

    public boolean start() {
        if (isBusy())
            return false;
        myThread = new Thread(this);
        myThread.start();
        return true;
    }

    @Override
    public void run()  {
        if (selfCheck)
            selfCheck();

        if (autoGetSelfUpdate)
            downloadSelfUpdate();

        if (check)
            check();

        if (autoGetUpdate)
            downloadUpdate();

        if (upgrade) {
            doUpgrade();
            upgrade = false;
        }
        notifyDone();
    }

    // --- --- --- --- --- --- --- --- Update --- --- --- --- --- --- --- --- --- ---

    private void selfCheck() {
        selfUpdate = null;
        boolean success;

        updater.setUrl("http://dev.m-lessmann.de/updater/updateConfig.json");
        if (success = updater.getConfig()) {
            if (success = updater.readConfig()) {

                List<IIndexType> types = updater.getSucceededTypes();

                String v = de.mlessmann.updating.updater.INFO.updaterVersion;

                for (IIndexType t : types) {
                    for (IRelease r : t.releases()) {
                        if (r.compareTo(v) == -1) {
                            v = r.version();
                            selfUpdate = r;
                        }
                    }
                }

            }
        }
        notifySelfCheckDone(success);
    }

    private void downloadSelfUpdate() {
        int[] count = new int[]{0,0};
        if (selfUpdate != null) {
            new File("cache/updates/updater").mkdirs();

            Map<String, String> files = selfUpdate.files();

            files.forEach((k, v) -> {
                try {
                    File file = new File("cache/updates/updater/" + k);
                    recvLog(this, FINE, "Downloading \"" + k + "\" from \"" + v + "\"");
                    HTTP.GETFILE(v, file);
                    count[1]++;
                } catch (IOException e) {
                    count[0]++;
                    recvLog(this, Level.INFO, "Unable to download file \"" + k + "\": " + e.toString());
                    recvExc(e);
                }
            });
        }
        notifySelfUpdateDownloaded(count[0], count[1]);
    }

    private void check() {
        selfUpdate = null;
        boolean success;

        updater.setUrl("http://dev.m-lessmann.de/hwserver/updateConfig.json");
        if (success = updater.getConfig()) {
            if (success = updater.readConfig()) {

                List<IIndexType> types = updater.getSucceededTypes();

                String v = HWServer.VERSION;
                recvLog(this, FINER, "Initial version: " + v);

                if (types.isEmpty()) {
                    recvLog(this, Level.WARNING, "No indexType reported success!");
                }
                for (IIndexType t : types) {
                    if (branch != null && !t.supportsBranching()) {
                        recvLog(this, Level.FINEST, "skipping type for not supporting branches");
                        continue;
                    }
                    if (t.releases().isEmpty())
                        recvLog(this, FINER, t.getIdentifier() + " has no release");

                    for (IRelease r : t.releases()) {
                        if (r.isDraft() && !drafts) {
                            recvLog(this, Level.WARNING, "skipping draft");
                            continue;
                        }
                        if (r.isPreRelease() && !preReleases) {
                            recvLog(this, Level.WARNING, "skipping preRelease");
                            continue;
                        }
                        if (branch != null && !r.branch().equals(branch)) {
                            recvLog(this, Level.WARNING, "skipping unselected branch");
                            continue;
                        }

                        recvLog(this, Level.FINEST, r.version());
                        if (r.compareTo(v) == -1) {
                            v = r.version();
                            update = r;
                        }
                    }
                }
            }
        }
        notifyCheckDone(success);
    }

    private void downloadUpdate() {
        int[] count = new int[]{0,0};
        if (update != null) {
            new File("cache/updates/server").mkdirs();

            Map<String, String> files = update.files();
            files.forEach((k, v) -> {
                try {
                    File file = new File("cache/updates/server/" + k);
                    recvLog(this, FINE, "Downloading \"" + k + "\" from \"" + v + "\"");
                    HTTP.GETFILE(v, file);
                    count[1]++;
                } catch (IOException e) {
                    count[0]++;
                    recvLog(this, Level.INFO, "Unable to download file \"" + k + "\": " + e.toString());
                    recvExc(e);
                }
            });
        }
        notifyUpdateDownloaded(count[0], count[1]);
    }

    private void doUpgrade() {
        if (update == null) {
            recvLog(this, WARNING, "No upgrade found.");
            return;
        }

        File zipFile = new File("cache/updates/server/update_" + update.version());

        if (!zipFile.isFile()){
            recvLog(this, SEVERE, "Upgrade not cached.");
            return;
        }

        File dir = new File("test").getParentFile();

        unzipFile(dir, zipFile);
    }

    // --- --- --- --- --- --- --- --- Archives --- --- --- --- --- --- --- --- --- ---

    public void unzipFile(File directory, File zipFile) {

        byte[] buffer = new byte[1024];

        if (!directory.isDirectory())
            directory.mkdirs();

        if (!zipFile.isFile()) {
            recvLog(this, SEVERE, "Unable to ");
        }

        try {
            ZipInputStream ziStrm = new ZipInputStream(new FileInputStream(zipFile));

            ZipEntry e = ziStrm.getNextEntry();

            while (e != null) {
                String name = e.getName();
                File fil = new File(directory, name);

                if (!e.isDirectory()) {
                    FileOutputStream outStrm = new FileOutputStream(fil);

                    int len;
                    while ((len = ziStrm.read(buffer)) > 0)
                        outStrm.write(buffer, 0, len);
                    outStrm.close();
                } else {
                    fil.mkdirs();
                }
                ziStrm.closeEntry();
                e = ziStrm.getNextEntry();
            }
            ziStrm.close();

        } catch (IOException e) {
            recvLog(this, SEVERE, "Unable to unpack file \"" + zipFile.getAbsolutePath() + "\": " + e.toString());
            recvExc(e);
        }
    }

    // --- --- --- --- --- --- --- --- ILogReceiver --- --- --- --- --- --- --- --- ---

    @Override
    public void recvLog(Object sender, Level level, String msg) {
        synchronized (server.getLogger()) {
            server.getLogger().log(level,
                    (sender == this ? "UpdateSvc" : (sender == updater ? "Updater" : sender.toString())) + ' ' + msg);
        }
    }

    @Override
    public void recvExc(Exception e) {
        e.printStackTrace();
    }

    // --- --- --- --- --- --- --- --- Callbacks --- --- --- --- --- --- --- --- ---

    private void notifySelfCheckDone(boolean success) {
        for (int i = listeners.size()-1; i>=0; i--)
            listeners.get(i).onUpdate_SelfCheckDone(success);
    }
    private void notifySelfUpdateDownloaded(int fails, int success) {
        for (int i = listeners.size()-1; i>=0; i--)
            listeners.get(i).onUpdate_SelfUpdateDownloaded(fails, success);
    }
    private void notifyCheckDone(boolean success) {
        for (int i = listeners.size()-1; i>=0; i--)
            listeners.get(i).onUpdate_CheckDone(success);
    }
    private void notifyUpdateDownloaded(int fails, int success) {
        for (int i = listeners.size()-1; i>=0; i--)
            listeners.get(i).onUpdate_UpdateDownloaded(fails, success);
    }
    private void notifyDone() {
        for (int i = listeners.size()-1; i>=0; i--)
            listeners.get(i).onUpdate_Done();
    }

    public void registerListener(IUpdateSvcReceiver l) {
        listeners.add(l);
    }

    public void unregisterListener(IUpdateSvcReceiver l) {
        listeners.remove(l);
    }

    // --- --- --- --- --- --- --- --- Setup help --- --- --- --- --- --- --- --- ---

    public UpdateSvc includeSelfCheck() { selfCheck = true; return this; }
    public UpdateSvc excludeSelfCheck() { selfCheck = false; return this; }
    public UpdateSvc downlSelfUpdate() { autoGetSelfUpdate = true; return this; }
    public UpdateSvc notGetSelfUpdate() { autoGetSelfUpdate = false; return this; }
    public UpdateSvc includeCheck() { check = true; return this; }
    public UpdateSvc excludeCheck() { check = false; return this; }
    public UpdateSvc downlUpdate() { autoGetUpdate = true; return this; }
    public UpdateSvc notGetUpdate() { autoGetUpdate = false; return this; }

    public UpdateSvc upgrade() { upgrade = true; return this; }

    public UpdateSvc branch(@Nullable String branch) {
        this.branch = branch;
        return this;
    }
    public UpdateSvc includePreReleases() { preReleases = true; return this; }
    public UpdateSvc excludePreReleases() { preReleases = false; return this; }
    public UpdateSvc includeDrafts() { drafts = true; return this; }
    public UpdateSvc excludeDrafts() { drafts = false; return this; }

    //Leadhammer
    public UpdateSvc full() {
        return this
                .includeSelfCheck()
                .downlSelfUpdate()
                .includeCheck()
                .downlUpdate()
                .excludePreReleases()
                .excludeDrafts()
                .branch(null);//TODO: Change default branch to "release"
    }

    // --- --- --- --- --- --- --- --- --- INFO --- --- --- --- --- --- --- --- --- ---

    public boolean hasSelfUpdate() { return selfUpdate != null; }
    public IRelease getSelfUpdate() { return selfUpdate; }
    public boolean hasUpdate() { return update != null; }
    public IRelease getUpdate() { return update; }

}

