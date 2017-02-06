package de.homeworkproject.server.hwserver.services.sessionsvc;

import de.homeworkproject.server.hwserver.HWServer;
import de.mlessmann.common.L4YGRandom;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Life4YourGames on 01.09.16.
 */
public class SessionMgrSvc {

    public Map<String, ClientSession> sessionsByToken = new HashMap<String, ClientSession>();
    private HWServer master;

    public SessionMgrSvc(HWServer master) {
        this.master = master;
    }

    //--------------------------- Sessions -----------------------------------------------------------------------------

    public synchronized void addSession(ClientSession s) {
        if (!sessionsByToken.containsValue(s))
            sessionsByToken.put(s.getToken().getToken(), s);
    }

    public synchronized Optional<ClientSession> getSession(String token) {
        return Optional.ofNullable(sessionsByToken.getOrDefault(token, null));
    }

    public synchronized Optional<ClientSession> getSession(SessionToken token) {
        return getSession(token.getToken());
    }

    public synchronized void delSession(String token) {
        sessionsByToken.remove(token);
    }

    public synchronized void delSession(SessionToken token) {
        delSession(token.getToken());
    }

    //--------------------------- Token generation ---------------------------------------------------------------------

    public SessionToken requestUniqueToken() {
        int i = 0;
        SessionToken t;
        do {
            t = requestToken();
            i++;
        } while (sessionsByToken.containsKey(t.getToken()));
        return t;
    }

    public SessionToken requestToken() {
        L4YGRandom.initRndIfNotAlready();
        String sToken = L4YGRandom.genRandomString(L4YGRandom.ALPHANUMERIC, 128);
        LocalDateTime expires = LocalDateTime.now().plusHours(5);
        return new SessionToken(sToken, expires);
    }

}
