package de.mlessmann.authentication;

import java.util.HashMap;
import java.util.Optional;

/**
 * Created by Life4YourGames on 26.06.16.
 */
public class AuthProvider {

    private static HashMap<String, IAuthMethod> methods = new HashMap<String, IAuthMethod>();

    public static IAuthMethod getDefault() {

        return new nativeAuthMethodPlaintext();

    }

    public static Optional<IAuthMethod> getMethod(String ident) {

        if ("default".equals(ident)) {

            return Optional.of(getDefault());

        }


        IAuthMethod res = null;

        if (methods.containsKey(ident)) {
            res = methods.get(ident);
        }

        //TODO: Implement method loading!

        return Optional.ofNullable(res);

    }

}
