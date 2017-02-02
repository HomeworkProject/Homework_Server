package de.mlessmann.hwserver.network.commands;

import de.mlessmann.hwserver.main.HWServer;
import de.mlessmann.hwserver.network.HWClientCommandContext;
import de.mlessmann.hwserver.network.Status;
import de.mlessmann.hwserver.reflections.HWCommandHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Life4YourGames on 30.06.16.
 */

@HWCommandHandler
public class nativeCommListCommands extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.listCommands";
    public static final String COMMAND = "listcommandhandler";
    private HWServer hwInstance = null;

    public nativeCommListCommands(HWServer server) {
        super();
        setID(IDENTIFIER);
        setCommand(COMMAND);
        hwInstance = server;

    }

    public CommandResult onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        ArrayList<ICommandHandler> handler;

        if (context.getRequest().has("search")) {
            handler = hwInstance.getCommandHandlerProvider().getByCommand(context.getRequest().getString("search"));
        } else {
            handler = hwInstance.getCommandHandlerProvider().getHandler();
        }

        JSONArray arr = new JSONArray();
        for (ICommandHandler h : handler) {
            JSONObject hO = new JSONObject();
            hO.put("id", h.getIdentifier());
            hO.put("command", h.getCommand());
            hO.put("isCritical", h.isCritical());
            arr.put(hO);
        }
        JSONObject r = Status.state_OK();
        r.put("payload_type", "JSONArray");
        r.put("array_type", "CommHandlerReference");
        r.put("payload", arr);
        r.put("commID", context.getHandler().getCurrentCommID());
        sendJSON(r);

        return CommandResult.success();
    }
}
