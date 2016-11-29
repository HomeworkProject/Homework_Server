package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWPermission;
import de.mlessmann.allocation.HWUser;
import de.mlessmann.homework.HWAttachmentLocation;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.network.Error;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.network.Types;
import de.mlessmann.perms.Permission;
import de.mlessmann.reflections.HWCommandHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Optional;

/**
 * Created by Life4YourGames on 06.11.16.
 */
@HWCommandHandler
public class nativeCommStoreAsset extends nativeCommandParent {

    private static final String ID = "de.mlessmann.commands.storeasset";
    private static final String COMM = "postasset";

    private HWServer server;

    public nativeCommStoreAsset(HWServer server) {
        super();
        this.server = server;
        setID(ID);
        setCommand(COMM);
    }

    @Override
    public CommandResult onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        //Check if file upload is enabled
        if (!server.getTCPServer().getFTManager().isEnabled()) {
            JSONObject resp = Status.state_ERROR(
                    Status.UNAVAILABLE,
                    Status.state_genError(
                            Error.Unavailable,
                            "File transfer is disabled",
                            "The server neither accepts nor sends files!"
                    )
            );
            sendJSON(resp);
            return CommandResult.clientFail();
        }

        if (!requireUser(context.getHandler())) {
            return CommandResult.clientFail();
        }

        //Checked by previous condition
        HWUser user = context.getHandler().getUser().get();
        Optional<HWPermission> perm = user.getPermission(Permission.HW_ATTACH);

        boolean allowed = perm.isPresent() && perm.get().getValue(Permission.HASVALUE) > 0;

        if (!allowed) {
            JSONObject resp = Status.state_ERROR(
                    Status.FORBIDDEN,
                    Status.state_genError(
                            Error.InsuffPerm,
                            "Not allowed to attach files to HW",
                            "You're not authorized to upload files to the server!"
                    )
            );
            sendJSON(resp);
            return CommandResult.clientFail();

        } else {
            JSONObject location = context.getRequest().optJSONObject("location");

            if (location==null) {
                JSONObject resp = Status.state_ERROR(
                        Status.BADREQUEST,
                        Status.state_genError(
                                Error.BadRequest,
                                "The request is missing a HWLocationObject",
                                "The client sent an invalid request"
                        )
                );
                sendJSON(resp);
                return CommandResult.clientFail();
            }
            HWAttachmentLocation loc = new HWAttachmentLocation(location);
            if (loc.getType() == HWAttachmentLocation.LocationType.SERVER) {
                return onServer(context, location);
            } else if (loc.getType() == HWAttachmentLocation.LocationType.WEB) {
                return onWeb(context, loc);
            } else {
                JSONObject resp = Status.state_ERROR(
                        Status.BADREQUEST,
                        Status.state_genError(
                                Error.BadRequest,
                                "The HWLocationObject is invalid",
                                "The client sent an invalid request"
                        )
                );
                sendJSON(resp);
                return CommandResult.clientFail();
            }
        }
    }

    private CommandResult onServer(HWClientCommandContext context, JSONObject location) {
        HWUser user = context.getHandler().getUser().get();
        //20kb default file size limit
        final int byteLimit = server.getConfig().getNode("limit", "maxAttachmentSize").optInt(20000);
        JSONArray date = location.optJSONArray("date");
        String hwID = location.optString("ownerhw");
        String name = location.optString("name");
        int approxSize = location.optInt("size", -1);

        if (date == null || date.length() < 3 || hwID == null || name == null || name.length() < 3) {
            JSONObject resp = Status.state_ERROR(
                    Status.BADREQUEST,
                    Status.state_genError(
                            Error.BadRequest,
                            "Either size, date or ID are invalid",
                            "The client sent an invalid request"
                    )
            );
            sendJSON(resp);
            return CommandResult.clientFail();
        }
        if (approxSize > byteLimit) {
            sendExceeded();
            return CommandResult.clientFail();
        }

        Optional<HomeWork> optHW = user.getHW(date.getInt(0), date.getInt(1), date.getInt(2), hwID);
        if (!optHW.isPresent()) {
            JSONObject resp = Status.state_ERROR(
                    Status.NOTFOUND,
                    Status.state_genError(
                            Error.NotFound,
                            "Specified HomeWork does not exist!",
                            "The client wanted to attach to a nonexistent HomeWork"
                    )
            );
            sendJSON(resp);
            return CommandResult.clientFail();
        }

        HomeWork hw = optHW.get();
        String id = hw.getNewAttachmentID();
        File file = new File(hw.getFile().getAbsoluteFile().getParent(), hw.getFile().getName() + File.pathSeparator + id);

        Optional<String> optToken = server.getTCPServer().getFTManager().requestTransferApproval(file, true);
        if (!optToken.isPresent()) {
            JSONObject resp = Status.state_ERROR(
                    Status.LOCKED,
                    Status.state_genError(
                            Error.Unauthorized,
                            "The file manager didn't authorize the transfer",
                            "Unable to upload file"
                    )
            );
            sendJSON(resp);
            return CommandResult.clientFail();
        }

        JSONObject resp = new JSONObject();
        resp.put("status", Status.OK);
        resp.put("payload_type", Types.FTInfo);
        JSONObject ftInfo = new JSONObject();
        ftInfo.put("token", optToken.get());
        ftInfo.put("direction", "POST");
        ftInfo.put("port", server.getTCPServer().getFtPort());
        resp.put("payload", ftInfo);
        sendJSON(resp);
        return CommandResult.success();
    }

    private CommandResult onWeb(HWClientCommandContext context, HWAttachmentLocation loc) {
        HWUser user = context.getHandler().getUser().get();

        Optional<HomeWork> optHW = user.getHW(loc.getDate().getYear(), loc.getDate().getMonthValue(), loc.getDate().getDayOfMonth(), loc.getHWID());
        if (!optHW.isPresent()) {
            JSONObject resp = Status.state_ERROR(
                    Status.NOTFOUND,
                    Status.state_genError(
                            Error.NotFound,
                            "Specified HomeWork does not exist!",
                            "The client wanted to attach to a nonexistent HomeWork"
                    )
            );
            sendJSON(resp);
            return CommandResult.clientFail();
        }
        if (optHW.get().registerAttachment(loc)) {
            sendJSON(Status.state_OK());
            return CommandResult.success();
        } else {
            JSONObject o = Status.state_ERROR(
                    Status.INTERNALERROR,
                    Status.state_genError(
                            Error.AddHWError,
                            "Cannot attach attachment",
                            "An internal error occurred"
                    )
            );
            sendJSON(o);
            return CommandResult.serverFail();
        }
    }

    private void sendExceeded() {
        JSONObject resp = Status.state_ERROR(
                Status.BADREQUEST,
                Status.state_genError(
                        Error.LimitExceeded,
                        "The maximum attachment size has been exceeded",
                        "The client has exceeded a set limit"
                )
        );
        sendJSON(resp);
    }
}
