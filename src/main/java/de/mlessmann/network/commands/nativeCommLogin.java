package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWGroup;
import de.mlessmann.allocation.HWUser;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.util.Common;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

/**
 * Created by Life4YourGames on 28.06.16.
 */

@HWCommandHandler
public class nativeCommLogin extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.login";
    public static final String COMMAND = "login";

    public nativeCommLogin() {

        setID(IDENTIFIER);
        setCommand(COMMAND);

    }

    public boolean onMessage(HWClientCommandContext context) {

        if (!require(context.getRequest(), "parameters", context.getHandler())) {

            return false;

        }

        JSONArray arr = context.getRequest().getJSONArray("parameters");

        String group = null;
        String user = null;
        String auth = null;

        if (arr.length() >= 1) {

            group = arr.getString(0);

        } else {

            JSONObject response = Status.state_ERROR(
                    Status.BADREQUEST,
                    Status.state_genError(
                            "ProtocolError",
                            "SetGroup needs at least 1 parameter",
                            "Client request was incomplete"
                    ));

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(context.getHandler(), response);
            return false;

        }

        if (arr.length() > 2) {

            user = arr.getString(1);
            auth = arr.getString(2);

        } else {

            user = "default";

            if (arr.length() == 2) {

                auth = arr.getString(1);

            } else {

                auth = "default";

            }
        }

        Optional<HWGroup> hwGroup = context.getHandler().requestGroup(group);

        if (!hwGroup.isPresent()) {

            context.getHandler().setUser(null);

            JSONObject response = Status.state_ERROR(
                    Status.NOTFOUND,
                    Status.state_genError(
                            "NotFoundError",
                            "Group " + group + " wasn't found",
                            "Group \"" + group + "\" does not exist"
                    ));

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(context.getHandler(), response);
            return false;

        }

        Optional<HWUser> hwUser = hwGroup.get().getUser(user, auth);

        if (!hwUser.isPresent()) {

            context.getHandler().setUser(null);

            JSONObject response = Status.state_ERROR(
                    Status.NOTFOUND,
                    Status.state_genError(
                            "NotFoundError",
                            "User \"" + user + "\" wasn't found",
                            "User \"" + user + "\" does not exist"
                    ));

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(context.getHandler(), response);
            return false;

        }

        context.getHandler().setUser(hwUser.get());

        JSONObject response = Status.state_OK();
        response.put("commID", context.getHandler().getCurrentCommID());

        sendJSON(context.getHandler(), response);

        return true;

    }
}
