package de.homeworkproject.server.network.commands;

import de.homeworkproject.server.allocation.GroupMgrSvc;
import de.homeworkproject.server.allocation.GroupSvc;
import de.homeworkproject.server.hwserver.HWServer;
import de.homeworkproject.server.network.Error;
import de.homeworkproject.server.network.HWClientCommandContext;
import de.homeworkproject.server.network.HWTCPClientReference;
import de.homeworkproject.server.network.Status;
import de.homeworkproject.server.reflections.HWCommandHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;

/**
 * Created by Life4YourGames on 21.09.16.
 */
@HWCommandHandler
public class nativeCommList extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.list";
    public static final String COMMAND = "list";

    private HWServer server;

    public nativeCommList(HWServer server) {
        super();
        this.server = server;
        setID(IDENTIFIER);
        setCommand(COMMAND);
    }

    @Override
    public CommandResult onMessage(HWClientCommandContext ctx) {
        super.onMessage(ctx);

        JSONObject req = ctx.getRequest();
        HWTCPClientReference handler = ctx.getHandler();

        if (req.has("group")) {
            Object gno = req.get("group");
            if (gno instanceof String) {
                return forGroup(((String) gno), ctx);
            }
        }
        return forAll(ctx);
    }

    private CommandResult forAll(HWClientCommandContext ctx) {
        GroupMgrSvc svc = server.getGroupManager();
        List<GroupSvc> grps = svc.getGroups();

        JSONObject jGrps = new JSONObject();
        grps.forEach(g -> {
            JSONArray users = new JSONArray();
            g.getUsers().forEach(u -> users.put(u.getUserName()));
            jGrps.put(g.getName(), users);
        });
        JSONObject resp = Status.state_OK();
        resp.put("payload_type", "HWGroupUserListing");
        resp.put("payload", jGrps);
        resp.put("commID", ctx.getHandler().getCurrentCommID());

        sendJSON(resp);
        return CommandResult.success();
    }

    private CommandResult forGroup(String grp, HWClientCommandContext ctx) {
        GroupMgrSvc svc = server.getGroupManager();
        Optional<GroupSvc> oGrp = svc.getGroup(grp);

        if (!oGrp.isPresent()) {
            JSONObject response = Status.state_ERROR(
                    Status.NOTFOUND,
                    Status.state_genError(
                            Error.NotFound,
                            "Group " + grp + " wasn't found",
                            "Group \"" + grp + "\" does not exist"
                    ));

            response.put("commID", ctx.getHandler().getCurrentCommID());
            sendJSON(response);
            return CommandResult.serverFail();
        }
        GroupSvc g = oGrp.get();
        JSONObject jGrps = new JSONObject();
        JSONArray users = new JSONArray();
        g.getUsers().forEach(u -> users.put(u.getUserName()));
        jGrps.put(g.getName(), users);

        JSONObject resp = Status.state_OK();
        resp.put("payload_type", "HWGroupUserListing");
        resp.put("payload", jGrps);
        resp.put("commID", ctx.getHandler().getCurrentCommID());
        sendJSON(resp);
        return CommandResult.success();
    }
}
