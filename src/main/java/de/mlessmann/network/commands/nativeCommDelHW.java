package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWUser;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.perms.Permission;
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

    public static final String IDENTIFIER = "de.mlessmann.commands.delHW";
    public static final String COMMAND = "delhw";

    public nativeCommDelHW() {

        setID(IDENTIFIER);
        setCommand(COMMAND);

    }

    @Override
    public boolean onMessage(HWClientCommandContext context) {

        JSONObject request = context.getRequest();

        if (!require(request, "date", context.getHandler()) || !require(request, "id", context.getHandler())
                || !requireUser(context.getHandler())) {

            return false;

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
                e.put("error", "DelHWError");
                e.put("error_message", "HomeWork could not be deleted");
                e.put("friendly_message", "HomeWork wasn't deleted due to a server error");
                response.put("payload", e);

                response.put("commID", context.getHandler().getCurrentCommID());

                sendJSON(context.getHandler(), response);

                return false;

            } else if (success == 0) {

                JSONObject response = new JSONObject();

                response.put("status", Status.OK);
                response.put("payload_type", "null");
                response.put("commID", context.getHandler().getCurrentCommID());

                sendJSON(context.getHandler(), response);

                return true;

            } else if (success == 2) {

                JSONObject response = new JSONObject();

                response.put("status", Status.FORBIDDEN);
                response.put("payload_type", "error");

                JSONObject e = new JSONObject();
                e.put("error", "InsufficientPermissionError");
                e.put("error_message", "Insufficient permission to delete the homework");
                e.put("friendly_message", "You're not allowed to delete this homework");
                e.put("perm", "has:" + Permission.HW_DEL);
                response.put("payload", e);

                response.put("commID", context.getHandler().getCurrentCommID());

                sendJSON(context.getHandler(), response);

                return false;

            }

        } catch (JSONException ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
            e.put("error", "JSONError");
            e.put("error_message", ex.toString());
            e.put("friendly_message", "Client sent an invalid request");
            response.put("payload", e);

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(context.getHandler(), response);

            return false;

        } catch (DateTimeException ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
            e.put("error", "DateTimeException");
            e.put("error_message", ex.toString());
            e.put("status_message", "Client sent an invalid request");
            response.put("payload", e);

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(context.getHandler(), response);

            return false;

        }

        return false;

    }

}
