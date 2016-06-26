package de.mlessmann.authentication;

import de.mlessmann.hwserver.HWServer;

/**
 * Created by Life4YourGames on 26.06.16.
 */
public interface IAuthMethod {

    public abstract boolean authorize(String storedData, String input);

    public abstract String masqueradePass(String input);

    public abstract String getMethodIdentifier();

    public abstract void setHWInstance(HWServer server);

}
