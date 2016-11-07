package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWUser;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.network.Error;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.perms.Permission;
import de.mlessmann.reflections.HWCommandHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Created by Life4YourGames on 31.10.16.
 */
@HWCommandHandler
public class nativeCommEditHW extends nativeCommandParent{

    public static final String IDENTIFIER = "de.mlessmann.commands.edithw";
    public static final String COMMAND = "addhw";

    public nativeCommEditHW() {
        super();
        setID(IDENTIFIER);
        setCommand(COMMAND);
    }

    @Override
    public boolean onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        if (!require(context.getRequest(), "homework", context.getHandler())) {
            return true;
        }

        if (!requireUser(context.getHandler())) {
            return true;
        }

        JSONObject hwObj = context.getRequest().getJSONObject("homework");

        JSONObject p = Status.state_PROCESSING();
        p.put("commID", context.getHandler().getCurrentCommID());
        sendJSON(p);

        if (!HomeWork.checkValidity(hwObj)) {

            JSONObject response = Status.state_ERROR(
                    Status.BADREQUEST,
                    Status.state_genError(
                            Error.EditHWError,
                            "Submitted HomeWork was invalid",
                            "Client submitted an invalid object"
                    )
            );

            response.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(response);
            return true;
        }

        LocalDate oldDate;
        try {
            JSONArray date = context.getRequest().getJSONArray("date");
            oldDate = LocalDate.of(date.getInt(0), date.getInt(1), date.getInt(2));
        } catch (JSONException | DateTimeException e) {
            JSONObject resp = Status.state_ERROR(
                    Status.BADREQUEST,
                    Status.state_genError(
                            Error.BadRequest,
                            "OldDate is missing or invalid",
                            "Client submitted an invalid original date"
                    )
            );
            resp.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(resp);
            return true;
        }

        String oldID;
        try {
            oldID = context.getRequest().getString("id");
        } catch (JSONException e) {
            JSONObject resp = Status.state_ERROR(
                    Status.BADREQUEST,
                    Status.state_genError(
                            Error.BadRequest,
                            "OldID is missing or invalid",
                            "Client submitted an invalid original ID"
                    )
            );
            resp.put("commID", context.getHandler().getCurrentCommID());
            sendJSON(resp);
            return true;
        }



        Optional<HWUser> u = context.getHandler().getUser();
        //IsPresent checked in #requireUser(HWTCPClientReference) above
        //noinspection OptionalGetWithoutIsPresent
        HWUser myUser = u.get();

        int success = myUser.editHW(oldDate, oldID, hwObj);

        if (success == -1) {

            JSONObject response = Status.state_ERROR(
                    Status.INTERNALERROR,
                    Status.state_genError(
                            Error.EditHWError,
                            "HomeWork not added",
                            "HomeWork wasn't added due to a server error"
                    )
            );

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);

            return true;

        } else if (success == 0) {

            JSONObject response = new JSONObject();

            response.put("status", Status.CREATED);
            response.put("payload_type", "null");
            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);

            return true;

        } else if (success == 1) {

            JSONObject response = Status.state_ERROR(
                    Status.FORBIDDEN,
                    Status.state_genError(
                            Error.InsuffPerm,
                            "Insufficient permission to edit the homework",
                            "You don't have enough permission to edit a homework"
                    )
            );

            response.getJSONObject("payload").put("perm", "has:" + Permission.HW_ADD_EDIT);

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);

            return true;

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

            return true;

        } else if (success == 3) {

            JSONObject response = Status.state_ERROR(
                    Status.INTERNALERROR,
                    Status.state_genError(
                            Error.EditHWError,
                            "Unable to edit the homework",
                            "Server failed to edit this homework"
                    )
            );

            response.getJSONObject("payload").put("perm", "has:" + Permission.HW_ADD_EDIT);

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);

            return true;

        }

        return false;

    }

}
