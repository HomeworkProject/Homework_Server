package de.homeworkproject.server.network;

import de.homeworkproject.server.allocation.GroupSvc;
import de.homeworkproject.server.allocation.HWUser;
import de.homeworkproject.server.hwserver.services.sessionsvc.SessionMgrSvc;
import org.json.JSONObject;

import java.net.Socket;
import java.util.Optional;

/**
 * Created by Life4YourGames on 29.06.16.
 */
public class HWTCPClientReference {

    private HWTCPClientHandler myHandler = null;

    public HWTCPClientReference(HWTCPClientHandler handler) {
        myHandler = handler;
    }

    public void sendMessage(String message) {
        myHandler.sendMessage(message);
    }

    public void sendJSON(JSONObject json) {
        myHandler.sendJSON(json);
    }

    public int getCurrentCommID() {
        return myHandler.getCurrentCommID();
    }

    public Optional<GroupSvc> requestGroup(String name) {
        return myHandler.getMaster().getMaster().getGroupManager().getGroup(name);
    }

    public Optional<HWUser> getUser() { return myHandler.getUser(); }

    public void setUser(HWUser u) { myHandler.setUser(u); }

    public SessionMgrSvc getSessionMgr() {
        return myHandler.getSessionMgr();
    }

    public Socket getRawSocket() {
        return myHandler.getRawSocket();
    }

    public void runCommand(JSONObject command) {
        myHandler.processCommand(command);
    }

}
