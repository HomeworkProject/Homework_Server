package de.mlessmann.hwserver.services.updates;

/**
 * Created by Life4YourGames on 10.09.16.
 */
public interface IUpdateSvcReceiver {

    void onUpdate_SelfCheckDone(boolean success);
    void onUpdate_SelfUpdateDownloaded(int fails, int success);

    void onUpdate_CheckDone(boolean success);
    void onUpdate_UpdateDownloaded(int fails, int success);

    void onUpdate_Done();

}
