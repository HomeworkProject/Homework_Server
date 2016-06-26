package de.mlessmann.authentication;

import de.mlessmann.hwserver.HWServer;

/**
 * Created by Life4YourGames on 26.06.16.
 */
public class nativeAuthMethodPlaintext implements IAuthMethod {

    public static String ident = "de.mlessmann.hwserver.auth.plaintext";

    public boolean authorize(String data, String input) {

        return data.equals(input);

    }

    public String masqueradePass(String input) {

        return input;

    }

    public String getMethodIdentifier() {

        return ident;

    }

    public void setHWInstance(HWServer server) {}

}
