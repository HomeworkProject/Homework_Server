package de.mlessmann.network;

import de.mlessmann.allocation.HWGroup;
import de.mlessmann.allocation.HWUser;
import org.json.JSONObject;

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

    public Optional<HWGroup> requestGroup(String name) {

        return myHandler.getMaster().getMaster().getGroup(name);

    }

    public Optional<HWUser> getUser() { return myHandler.getUser(); }

    public void setUser(HWUser u) { myHandler.setUser(u); }

}
