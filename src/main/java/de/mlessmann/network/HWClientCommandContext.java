package de.mlessmann.network;

import org.json.JSONObject;

/**
 * Created by Life4YourGames on 28.06.16.
 */
public class HWClientCommandContext {

    private HWTCPClientReference myRef;
    private JSONObject myRequest;

    public HWClientCommandContext(JSONObject request, HWTCPClientReference ref) {
        myRef = ref;
        myRequest = request;
    }

    public JSONObject getRequest() { return myRequest; }

    public HWTCPClientReference getHandler() { return myRef; }

}
