package de.homeworkproject.server.authentication;

import de.homeworkproject.server.hwserver.HWServer;
import de.homeworkproject.server.reflections.HWAuthMethod;

/**
 * Created by Life4YourGames on 26.06.16.
 */
@HWAuthMethod
public class nativeAuthMethodPlaintext implements IAuthMethod {

    public static String ident = "de.mlessmann.hwserver.auth.plaintext";

    public boolean authorize(String data, String input) {
        if (data==null || data.isEmpty()) return false;
        return data.equals(input);
    }

    public String masqueradePass(String input) {
        return input;
    }

    public String getIdentifier() {
        return ident;
    }

    public void setHWInstance(HWServer server) {}

}
