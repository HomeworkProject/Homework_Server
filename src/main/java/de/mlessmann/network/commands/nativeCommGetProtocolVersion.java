package de.mlessmann.network.commands;

import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.reflections.HWCommandHandler;
import org.json.JSONObject;

/**
 * Created by Life4YourGames on 28.06.16.
 */

@HWCommandHandler
public class nativeCommGetProtocolVersion extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.getProtocolVersion";
    public static final String COMMAND = "getinfo";

    public nativeCommGetProtocolVersion() {
        setID(IDENTIFIER);
        setCommand(COMMAND);
    }

    @Override
    public boolean onMessage(HWClientCommandContext context) {

        JSONObject response = new JSONObject();
        response.put("protoVersion", Status.SCURRENTPROTOVERSION);

        sendJSON(context.getHandler(), response);

        return true;

    }

}
