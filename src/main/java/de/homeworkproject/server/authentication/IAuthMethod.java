package de.homeworkproject.server.authentication;

import de.homeworkproject.server.hwserver.HWServer;

/**
 * Created by Life4YourGames on 26.06.16.
 */
public interface IAuthMethod {

    boolean authorize(String storedData, String input);

    String masqueradePass(String input);

    String getIdentifier();

    void setHWInstance(HWServer server);

}
