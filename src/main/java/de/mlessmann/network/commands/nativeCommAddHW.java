package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWUser;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.network.Error;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.perms.Permission;
import de.mlessmann.reflections.HWCommandHandler;
import org.json.JSONObject;

import java.util.Optional;

/**
 * Created by Life4YourGames on 28.06.16.
 */

@HWCommandHandler
public class nativeCommAddHW extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.addhw";
    public static final String COMMAND = "addhw";

    public nativeCommAddHW() {
        super();
        setID(IDENTIFIER);
        setCommand(COMMAND);
    }

    @Override
    public CommandResult onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        if (!require(context.getRequest(), "homework", context.getHandler())) {
            return CommandResult.clientFail();
        }

        if (!requireUser(context.getHandler())) {
            return CommandResult.clientFail();
        }

        JSONObject hwObj = context.getRequest().getJSONObject("homework");

        JSONObject p = Status.state_PROCESSING();
        p.put("commID", context.getHandler().getCurrentCommID());
        sendJSON(p);

        if (!HomeWork.checkValidity(hwObj)) {
            JSONObject response = Status.state_ERROR(
                    Status.BADREQUEST,
                    Status.state_genError(
                            Error.AddHWError,
                            "Submitted HomeWork was invalid",
                            "Client submitted an invalid object"
                    )
            );
            response.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(response);

            return CommandResult.clientFail();
        }

        Optional<HWUser> u = context.getHandler().getUser();

        //IsPresent checked in #requireUser(HWTCPClientReference) above
        //noinspection OptionalGetWithoutIsPresent
        HWUser myUser = u.get();


        int success = myUser.addHW(hwObj);

        if (success == -1) {
            JSONObject response = Status.state_ERROR(
                    Status.INTERNALERROR,
                    Status.state_genError(
                            Error.AddHWError,
                            "HomeWork not added",
                            "HomeWork wasn't added due to a server error"
                    )
            );
            response.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(response);

            return CommandResult.clientFail();

        } else if (success == 0) {

            JSONObject response = Status.state_CREATED();
            sendJSON(response);

            return CommandResult.success();

        } else if (success == 1) {

            JSONObject response = Status.state_ERROR(
                    Status.FORBIDDEN,
                    Status.state_genError(
                            Error.InsuffPerm,
                            "Insufficient permission to add the homework",
                            "You don't have enough permission to add a homework"
                    )
            );
            response.getJSONObject("payload").put("perm", "has:" + Permission.HW_ADD_NEW);
            response.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(response);

            return CommandResult.clientFail();

        } else if (success == 2) {

            JSONObject response = Status.state_ERROR(
                    Status.FORBIDDEN,
                    Status.state_genError(
                            Error.InsuffPerm,
                            "Insufficient permission to edit the homework",
                            "You don't have enough permission to edit this homework"
                    )
            );
            response.getJSONObject("payload").put("perm", "has:" + Permission.HW_ADD_EDIT);
            response.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(response);

            return CommandResult.clientFail();

        }
        return CommandResult.unhandled();
    }

}
