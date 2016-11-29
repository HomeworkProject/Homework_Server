package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWPermission;
import de.mlessmann.allocation.HWUser;
import de.mlessmann.homework.HWAttachment;
import de.mlessmann.homework.HWAttachmentLocation;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.network.Error;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.network.Types;
import de.mlessmann.perms.Permission;
import de.mlessmann.reflections.HWCommandHandler;
import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Created by Life4YourGames on 10.11.16.
 */
@HWCommandHandler
public class nativeCommReceiveAsset extends nativeCommandParent {

    private static final String ID = "de.mlessmann.commands.getasset";
    private static final String COMM = "getasset";

    private HWServer hwServer;

    public nativeCommReceiveAsset(HWServer hwServer) {
        super();
        this.hwServer = hwServer;
        setID(ID);
        setCommand(COMM);
    }

    @Override
    public CommandResult onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        //Check if file download is enabled
        if (!hwServer.getTCPServer().getFTManager().isEnabled()) {
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

            HWAttachmentLocation a = new HWAttachmentLocation(context.getRequest().optJSONObject("location"));
            if (a.getType() == HWAttachmentLocation.LocationType.INVALID) {
                JSONObject o = Status.state_ERROR(
                        Status.BADREQUEST,
                        Status.state_genError(
                                Error.BadRequest,
                                "HWAttachmentLocation invalid",
                                "Client sent an invalid request"
                        )
                );
                sendJSON(o);
                return CommandResult.clientFail();
            }

            if (a.getType() == HWAttachmentLocation.LocationType.WEB) {
                JSONObject o = Status.state_ERROR(
                        Status.NOCONTENT,
                        Status.state_genError(
                                Error.NotFound,
                                "HWAttachmentLocation not on server",
                                "Client asked for a remote document"
                        )
                );
                sendJSON(o);
                return CommandResult.clientFail();
            }

            //a.getType() == HWAttachmentLocation.LocationType.SERVER
            LocalDate date = a.getDate();
            String hwID = a.getHWID();
            Optional<HomeWork> optHW = user.getHW(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), hwID);
            if (!optHW.isPresent()) {
                JSONObject resp = Status.state_ERROR(
                        Status.NOTFOUND,
                        Status.state_genError(
                                Error.NotFound,
                                "HomeWork doesn't exist",
                                "The specified homework does not exist"
                        )
                );
                sendJSON(resp);
                return CommandResult.clientFail();
            }
            HomeWork hw = optHW.get();

            Optional<HWAttachment> optAttachment = hw.getAttachment(a.getAssetID());

            if (optAttachment.isPresent()) {
                JSONObject resp = Status.state_ERROR(
                        Status.NOTFOUND,
                        Status.state_genError(
                                Error.NotFound,
                                "HomeWork doesn't exist",
                                "The specified homework does not exist"
                        )
                );
                sendJSON(resp);
                return CommandResult.clientFail();
            }

            HWAttachment attachment = optAttachment.get();

            File attachFile = new File(hw.getFile().getAbsoluteFile().getParent(), hw.getID() + File.separatorChar + attachment.getID());
            Optional<String> optToken = hwServer.getTCPServer().getFTManager().requestTransferApproval(attachFile, false);
            if (!optToken.isPresent()) {
                JSONObject resp = Status.state_ERROR(
                        Status.LOCKED,
                        Status.state_genError(
                                Error.Unauthorized,
                                "Server didn't authorize transfer",
                                "The server rejected the transfer request"
                        )
                );
                sendJSON(resp);
                return CommandResult.clientFail();
            } else {
                JSONObject resp = new JSONObject();
                resp.put("status", Status.OK);
                resp.put("payload_type", Types.FTInfo);
                JSONObject ftInfo = new JSONObject();
                ftInfo.put("token", optToken.get());
                ftInfo.put("direction", "GET");
                ftInfo.put("port", hwServer.getTCPServer().getFtPort());
                resp.put("payload", ftInfo);
                sendJSON(resp);
                return CommandResult.success();
            }
        }
    }
}
