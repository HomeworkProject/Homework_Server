package de.mlessmann.hwserver.services.updates;

import de.mlessmann.common.annotations.Nullable;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.updater.IUpdateStateListener;
import de.mlessmann.updater.Updater;
import de.mlessmann.updater.reflect.IRelease;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 10.09.16.
 */
public class UpdateSvc implements Runnable, IUpdateStateListener {

    private HWServer server;

    private Thread myThread;

    private Updater updater;

    //Svc configuration
    private boolean check;
    private boolean autoGetUpdate;
    private boolean autoUpgrade;

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

    }

    // --- --- --- --- --- --- --- --- IUpdateStateListener --- --- --- --- --- --- --- --- ---

    @Override
    public void onMessage(Object sender, Level level, String msg) {
        synchronized (server.getLogger()) {
            server.getLogger().log(level,
                    (sender == this ? "UpdateSvc" : (sender == updater ? "Updater" : sender.toString())) + ' ' + msg);
        }
    }

    @Override
    public void onException(Object sender, Level level, Exception e) {
        e.printStackTrace();
    }

    // --- --- --- --- --- --- --- --- Callbacks --- --- --- --- --- --- --- --- ---

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

