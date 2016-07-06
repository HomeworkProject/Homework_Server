package de.mlessmann.authentication;

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 26.06.16.
 */
public class AuthProvider {

    private HashMap<String, IAuthMethod> methods = new HashMap<String, IAuthMethod>();
    private Logger myLogger = Logger.getGlobal();

    public HashMap<String, IAuthMethod> getMethods() {

        HashMap<String, IAuthMethod> res = new HashMap<String, IAuthMethod>();

        methods.forEach(res::put);

        return res;

    }

    public IAuthMethod getDefault() {

        return new nativeAuthMethodPlaintext();

    }

    public Optional<IAuthMethod> getMethod(String ident) {

        if ("default".equals(ident)) {

            return Optional.of(getDefault());

        }


        IAuthMethod res = null;

        if (methods.containsKey(ident)) {
            res = methods.get(ident);
        }

        return Optional.ofNullable(res);

    }

    public void registerMethod(IAuthMethod m) {

        myLogger.finest("Registering command " + m.getIdentifier());

        if (!getMethod(m.getIdentifier()).isPresent()) {

            methods.put(m.getIdentifier(), m);
            myLogger.finer("Registered command " + m.getIdentifier());

        } else {

            myLogger.warning("Command " + m.getIdentifier() + " already registered!");

        }
    }

    public void setLogger(Logger l) { myLogger = l; }

}
