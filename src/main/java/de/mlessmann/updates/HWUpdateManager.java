package de.mlessmann.updates;

import de.mlessmann.common.annotations.Nullable;
import de.mlessmann.common.parallel.IFuture;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.updates.indices.IRelease;

import java.util.logging.Level;

/**
 * Created by Life4YourGames on 07.10.16.
 */
public class HWUpdateManager {

    private HWServer server;

    // --- Futures ---
    //Update
    private IRelease updateResult = null;
    private IFuture<IRelease> updateFuture = null;
    private Thread updateThread = null;
    private HWUpdate lastUpdate = null;

    //Upgrade
    private Boolean upgradeResult = null;
    private IFuture<Boolean> upgradeFuture = null;
    private Thread upgradeThread = null;
    private HWUpgrade lastUpgrade = null;

    public HWUpdateManager(HWServer server) {
        this.server = server;
    }

    @Nullable
    public synchronized IFuture<IRelease> checkForUpdate() {
        if (updateFuture!=null || updateThread!=null)
            return null;

        String confRev = server.getConfig().getNode("update", "confRev").optString(HWUpdate.DEFREV);
        String confURI = server.getConfig().getNode("update", "confURI").optString(HWUpdate.DEFURL);

        updateResult =null;
        updateFuture = new HWUpdateFuture<IRelease>(future -> updateResult);
        updateThread = new Thread(() -> {
            try {
                //Ensure that the future has been returned
                Thread.sleep(1000);
                lastUpdate = new HWUpdate(server);
                lastUpdate.setConfRev(confRev);
                lastUpdate.setConfUrl(confURI);

                if (!lastUpdate.check()) {
                    server.onMessage(this, Level.WARNING, "Update check failed!");
                    updateResult = null;
                } else {
                    server.onMessage(this, Level.FINE, "Update check done. Checking for newer release");
                    updateResult = lastUpdate.getNewer(HWServer.VERSION);
                }
            } catch (Exception e) {
                server.onMessage(this, Level.SEVERE, "An exception occurred while checking for updates: " + e.toString());
                server.onException(this, Level.SEVERE, e);
            }
            ((HWUpdateFuture) updateFuture).pokeListeners();
        });
        updateThread.start();

        return updateFuture;
    }

    @Nullable
    public IFuture<IRelease> getUpdateFuture() { return updateFuture; }

    @Nullable
    public HWUpdate getLastUpdate() { return lastUpdate; }


    @Nullable
    public synchronized IFuture<Boolean> upgrade(IRelease release) {
        if (upgradeFuture!=null || upgradeThread!=null)
            return null;

        upgradeResult = null;
        upgradeFuture = new HWUpdateFuture<Boolean>(future -> upgradeResult);
        upgradeThread = new Thread(() -> {
            try {
                //Ensure that the future has been returned
                Thread.sleep(1000);
                boolean skip = false;
                lastUpgrade = new HWUpgrade(server);
                lastUpgrade.setRelease(release);
                server.onMessage(this, Level.FINER, "Downloading files");
                if (!lastUpgrade.downloadFiles()) {
                    server.onMessage(this, Level.WARNING, "Unable to download files!");
                    upgradeResult = Boolean.FALSE;
                    skip = true;
                }
                if (!skip && !lastUpgrade.genConfig()) {
                    server.onMessage(this, Level.WARNING, "Unable to generate upgradeConfig!");
                    upgradeResult = Boolean.FALSE;
                    skip = true;
                }
                if (!skip) {
                    upgradeResult = Boolean.TRUE;
                }
            } catch (Exception e) {
                server.onMessage(this, Level.SEVERE, "An exception occurred while upgrading: " + e.toString());
                server.onException(this, Level.SEVERE, e);
                upgradeResult = Boolean.FALSE;
            }
            ((HWUpdateFuture) upgradeFuture).pokeListeners();
        });
        upgradeThread.start();

        return upgradeFuture;
    }


    @Nullable
    public IFuture<Boolean> getUpgradeFuture() { return upgradeFuture; }

    @Nullable
    public HWUpgrade getLastUpgrade() { return lastUpgrade; }

}