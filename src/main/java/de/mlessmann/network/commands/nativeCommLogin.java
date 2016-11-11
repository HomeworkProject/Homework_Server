package de.mlessmann.network.commands;

import de.mlessmann.allocation.GroupSvc;
import de.mlessmann.allocation.HWUser;
import de.mlessmann.hwserver.services.sessionsvc.ClientSession;
import de.mlessmann.hwserver.services.sessionsvc.SessionMgrSvc;
import de.mlessmann.network.Error;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.reflections.HWCommandHandler;
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
        super();
        setID(IDENTIFIER);
        setCommand(COMMAND);
    }

    public CommandResult onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        JSONObject request = context.getRequest();

        if (request.has("session")) {
            return byToken(context);
        } else {
            return byAuth(context);
        }

    }

    public CommandResult byToken(HWClientCommandContext context) {

        boolean valid = false;
        ClientSession cs = null;

        JSONObject request = context.getRequest();
        JSONObject session = request.getJSONObject("session");

        String token = session.getString("token");

        SessionMgrSvc svc = context.getHandler().getSessionMgr();

        Optional<ClientSession> s = svc.getSession(token);

        if (s.isPresent()) {
            cs = s.get();
            if (cs.getToken().isValid()) {
                Optional<HWUser> u = cs.getUser();
                Optional<GroupSvc> g = cs.getGroup();
                if (u.isPresent() && g.isPresent()) {
                    context.getHandler().setUser(u.get());
                    valid = true;
                }
            }
        }

        if (valid) {
            JSONObject response = Status.state_OK();
            JSONObject rS = cs.getToken().toJSON();
            rS.put("group", cs.getGroup().get().getName());
            rS.put("user", cs.getUser().get().getUserName());
            response.put("session", rS);
            sendJSON(response);
            return CommandResult.success();
        } else {
            JSONObject response = Status.state_ERROR(Status.EXPIRED,
                    Status.state_genError(
                            Status.SEXPIRED,
                            "Token invalid. May be expired.",
                            "Sorry, the supplied token is invalid"
                    ));

            sendJSON(response);
            return CommandResult.clientFail();
        }
    }

    public CommandResult byAuth(HWClientCommandContext context) {
        if (!require(context.getRequest(), "parameters", context.getHandler())) {
            return CommandResult.clientFail();
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
                            Error.BadRequest,
                            "SetGroup needs at least 1 parameter",
                            "Client request was incomplete"
                    ));

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);
            return CommandResult.clientFail();
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

        Optional<GroupSvc> hwGroup = context.getHandler().requestGroup(group);

        if (!hwGroup.isPresent()) {
            context.getHandler().setUser(null);

            JSONObject response = Status.state_ERROR(
                    Status.NOTFOUND,
                    Status.state_genError(
                            Error.NotFound,
                            "Group " + group + " wasn't found",
                            "Group \"" + group + "\" does not exist"
                    ));

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);
            return CommandResult.clientFail();
        }

        Optional<HWUser> hwUser = hwGroup.get().getUser(user);

        if (!hwUser.isPresent()) {
            context.getHandler().setUser(null);

            JSONObject response = Status.state_ERROR(
                    Status.NOTFOUND,
                    Status.state_genError(
                            Error.NotFound,
                            "User " + user + " wasn't found",
                            "User \"" + user + "\" does not exist"
                    ));

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);
            return CommandResult.clientFail();
        }

        boolean authorized = hwUser.get().authorize(auth);

        if (!authorized) {
            context.getHandler().setUser(null);

            JSONObject response = Status.state_ERROR(
                    Status.UNAUTHORIZED,
                    Status.state_genError(
                            Error.InvalidCred,
                            "Invalid credentials",
                            "Invalid credentials"
                    ));

            response.put("commID", context.getHandler().getCurrentCommID());

            sendJSON(response);
            return CommandResult.clientFail();
        }

        SessionMgrSvc svc = context.getHandler().getSessionMgr();
        ClientSession s = new ClientSession(svc);
        s.setUser(hwUser.get());
        s.setGroup(hwGroup.get());
        s.genSToken();

        context.getHandler().setUser(hwUser.get());

        JSONObject response = Status.state_OK();
        response.put("commID", context.getHandler().getCurrentCommID());

        if (s.getToken().isValid()) {
            JSONObject sJ = s.getToken().toJSON();
            sJ.put("user", hwUser.get().getUserName());
            sJ.put("group", hwGroup.get().getName());
            response.put("session", sJ);
            svc.addSession(s);
        }

        sendJSON(response);

        return CommandResult.success();

    }
}
