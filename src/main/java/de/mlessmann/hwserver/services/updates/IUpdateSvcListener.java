package de.mlessmann.hwserver.services.updates;

/**
 * Created by Life4YourGames on 17.09.16.
 */
public interface IUpdateSvcListener {

    void onSvcStart();

    void onSvcDone(boolean success);

    void onUpdateAvailable(IRelease r);

    void onNoUpdateAvailable();

    void onUpdateDownloaded();

    void onUpgradeAboutToStart(boolean immediate);

    void onUpgradeFailed();
}
