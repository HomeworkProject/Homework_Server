package de.homeworkproject.server.network.commands;

import de.homeworkproject.server.allocation.HWUser;
import de.homeworkproject.server.network.Error;
import de.homeworkproject.server.network.HWClientCommandContext;
import de.homeworkproject.server.network.Status;
import de.homeworkproject.server.perms.Permission;
import de.homeworkproject.server.reflections.HWCommandHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * Created by Life4YourGames on 28.06.16.
 */

@HWCommandHandler
public class nativeCommDelHW  extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.delhw";
    public static final String COMMAND = "delhw";

    public nativeCommDelHW() {
        super();
        setID(IDENTIFIER);
        setCommand(COMMAND);

    }

    @Override
    public CommandResult onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        JSONObject request = context.getRequest();

        if (!require(request, "date", context.getHandler()) || !require(request, "id", context.getHandler())
                || !requireUser(context.getHandler())) {
            return CommandResult.clientFail();
        }

        //IsPresent checked in #requireUser(HWTCPClientReference) above
        //noinspection OptionalGetWithoutIsPresent
        HWUser myUser = context.getHandler().getUser().get();

        try {

            JSONArray date = request.getJSONArray("date");

            String id = request.getString("id");

            LocalDate ldate = LocalDate.of(date.getInt(0), date.getInt(1), date.getInt(2));

            int success = myUser.delHW(ldate, id);

            if (success == 1) {

                JSONObject response = new JSONObject();
                response.put("status", Status.INTERNALERROR);
                response.put("payload_type", "error");
                JSONObject e = new JSONObject();
                e.put("error", Error.DelHWError);
                e.put("error_message", "HomeWork could not be deleted");
                e.put("friendly_message", "HomeWork wasn't deleted due to a server error");
                response.put("payload", e);
                response.put("commID", context.getHandler().getCurrentCommID());
                sendJSON(response);
                return CommandResult.serverFail();

            } else if (success == 0) {

                JSONObject response = new JSONObject();
                response.put("status", Status.OK);
                response.put("payload_type", "null");
                response.put("commID", context.getHandler().getCurrentCommID());
                sendJSON(response);
                return CommandResult.success();

            } else if (success == 3) {

                JSONObject response = new JSONObject();
                response.put("status", Status.FORBIDDEN);
                response.put("payload_type", "error");
                JSONObject e = new JSONObject();
                e.put("error", Error.InsuffPerm);
                e.put("error_message", "Insufficient permission to delete the homework");
                e.put("friendly_message", "You're not allowed to delete this homework");
                e.put("perm", "has:" + Permission.HW_DEL);
                response.put("payload", e);
                response.put("commID", context.getHandler().getCurrentCommID());
                sendJSON(response);

                return CommandResult.clientFail();
            }

        } catch (JSONException ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
            e.put("error", Error.BadRequest);
            e.put("error_message", ex.toString());
            e.put("friendly_message", "Client sent an invalid request");
            response.put("payload", e);

            response.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(response);

            return CommandResult.clientFail();

        } catch (DateTimeException ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
            e.put("error", Error.DateTimeError);
            e.put("error_message", ex.toString());
            e.put("status_message", "Client sent an invalid request");
            response.put("payload", e);

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);

            return CommandResult.clientFail();
        }
        return CommandResult.unhandled();
    }

}
