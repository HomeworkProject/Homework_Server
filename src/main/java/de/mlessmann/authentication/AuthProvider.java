package de.mlessmann.authentication;

/**
 * Created by Life4YourGames on 26.06.16.
 */
public class AuthProvider {

    public static IAuthMethod getDefault() {

        return new nativeAuthMethodPlaintext();

    }

    public static IAuthMethod getMethod(String ident) {

        if ("default".equals(ident)) {

            return getDefault();

        }

        //TODO: Implement method loading!

        return getDefault();

    }

}
