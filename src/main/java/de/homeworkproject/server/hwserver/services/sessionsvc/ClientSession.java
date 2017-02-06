package de.homeworkproject.server.hwserver.services.sessionsvc;

import de.homeworkproject.server.allocation.GroupSvc;
import de.homeworkproject.server.allocation.HWUser;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Life4YourGames on 01.09.16.
 */
public class ClientSession {

    private List<Socket> socks = new ArrayList<Socket>();

    private SessionToken token = null;
    private SessionMgrSvc sessionMgr;
    private HWUser user;
    private GroupSvc group;

    public ClientSession(SessionMgrSvc sessionMgr) {
        this.sessionMgr = sessionMgr;
    }


    //--------------------------- Sockets ------------------------------------------------------------------------------

    public void addSocket(Socket s) {
        if (!socks.contains(s))
            socks.add(s);
    }

    public List<Socket> getSocks() {
        return socks;
    }

    //--------------------------- Token  -------------------------------------------------------------------------------

    public ClientSession genSToken() {
        token = sessionMgr.requestUniqueToken();
        return this;
    }

    public boolean hasValidToken() {
        return token != null && token.isValid();
    }

    public SessionToken getToken() {
        return token;
    }

    //--------------------------- Allocation ---------------------------------------------------------------------------

    public void setUser(HWUser user) {
        this.user = user;
    }

    public void setGroup(GroupSvc group) {
        this.group = group;
    }

    public Optional<HWUser> getUser() { return Optional.of(user); }

    public Optional<GroupSvc> getGroup() { return Optional.of(group); }

}
