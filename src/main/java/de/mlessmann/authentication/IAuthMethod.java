package de.mlessmann.authentication;

import de.mlessmann.hwserver.HWServer;

/**
 * Created by Life4YourGames on 26.06.16.
 */
public interface IAuthMethod {

    boolean authorize(String storedData, String input);

    String masqueradePass(String input);

    String getIdentifier();

    void setHWInstance(HWServer server);

}
