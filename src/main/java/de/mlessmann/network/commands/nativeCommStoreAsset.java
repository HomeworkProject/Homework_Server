package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWPermission;
import de.mlessmann.allocation.HWUser;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.network.Error;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import de.mlessmann.perms.Permission;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Created by Life4YourGames on 06.11.16.
 */
public class nativeCommStoreAsset extends nativeCommandParent {

    private static final String ID = "de.mlessmann.commands.storeasset";
    private static final String COMM = "storeasset";

    private HWServer server;

    public nativeCommStoreAsset(HWServer server) {
        super();
        setID(ID);
        setCommand(COMM);
    }

    @Override
    public boolean onMessage(HWClientCommandContext context) {
        super.onMessage(context);

        if (!requireUser(context.getHandler())) {
            return true;
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
            return true;

        } else {
            //20kb default file size limit
            final int byteLimit = server.getConfig().getNode("limit", "maxAttachmentSize").optInt(20000);
            int approxSize = context.getRequest().optInt("size", -1);
            JSONArray date = context.getRequest().optJSONArray("date");
            String hwID = context.getRequest().optString("id");
            String name = context.getRequest().optString("name");

            if (approxSize == -1 || date==null || date.length() < 3 || hwID == null || name == null || name.length() < 3) {
                JSONObject resp  = Status.state_ERROR(
                        Status.BADREQUEST,
                        Status.state_genError(
                                Error.BadRequest,
                                "Either size, date or ID are invalid",
                                "The client sent an invalid request"
                        )
                );
                sendJSON(resp);
                return true;
            }
            if (approxSize > byteLimit) {
                sendExceeded();
                return true;
            }

            Optional<HomeWork> optHW= user.getHW(date.getInt(0), date.getInt(1), date.getInt(2), hwID);
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
                return true;
            }

            HomeWork hw = optHW.get();
            String id = hw.getNewAttachmentID();
            File file = new File(hw.getFile().getAbsoluteFile().getParent(), hw.getFile().getName() + File.pathSeparator + id);

            boolean exceeded = false;
            try (
                    FileOutputStream writer = new FileOutputStream(file)
            ) {
                InputStream s = context.getHandler().getRawSocket().getInputStream();
                JSONObject resp = Status.state_CONTINUE();
                sendJSON(resp);

                //WRAP SOCKET
                int readBytes = 0;
                int b = 0;
                byte[] buffer = new byte[2048];
                while ((b = s.read(buffer, 0, buffer.length)) > -1) {
                    if (readBytes > byteLimit) {
                        exceeded = true;
                        break;
                    }
                    writer.write(buffer);
                }
                writer.flush();


            } catch (IOException e) {
                JSONObject resp = Status.state_ERROR(
                        Status.INTERNALERROR,
                        Status.state_genError(
                                Error.InternalError,
                                "An internal error prevented writing the attachments file",
                                "An internal error occurred"
                        )
                );
                sendJSON(resp);
            }
            if (exceeded) {
                file.delete();
                sendExceeded();
                return true;
            }
            hw.registerAttachment(id, file);
            hw.flush();
        }
        return true;
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
