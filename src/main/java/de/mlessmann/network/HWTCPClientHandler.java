package de.mlessmann.network;

import de.mlessmann.allocation.HWUser;
import de.mlessmann.hwserver.services.sessionsvc.SessionMgrSvc;
import de.mlessmann.network.commands.ICommandHandler;
import de.mlessmann.network.commands._nativeCommTCPCHDummy;
import de.mlessmann.util.L4YGRandom;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;

import static de.mlessmann.util.Common.negateInt;

/**
 * Created by MarkL4YG on 08.05.2016.
 * @author MarkL4YG
 * (C 2016) Magnus Leßmann
 */
public class HWTCPClientHandler {

    private static ICommandHandler HANDLER_NATIVE = new _nativeCommTCPCHDummy();


    private HWTCPServer master;
    private Socket mySock;
    private BufferedReader reader;
    private BufferedWriter writer;
    private HWUser myUser;
    private HWTCPClientReference myReference;
    private ICommandHandler currentCommHandler = HANDLER_NATIVE;
    private boolean terminated = false;
    private boolean isClosed = false;
    private boolean greeted = false;
    private int currentCommID;

    public HWTCPClientHandler(Socket clientSock, HWTCPServer tcpServer) {

        this.mySock = clientSock;
        this.master = tcpServer;
        this.myReference = new HWTCPClientReference(this);

    }

    public boolean setUp() {

        try {

            reader = new BufferedReader(new InputStreamReader(mySock.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(mySock.getOutputStream()));

        } catch (IOException ex) {

            StringBuilder builder = new StringBuilder("Unable to get inputStream of Socket: ");
            builder.append(ex.toString());
            master.sendLog(this, Level.WARNING, builder.toString());
            return true;

        }

        return true;

    }

    public SessionMgrSvc getSessionMgr() { return master.getMaster().getSessionMgr(); }

    public Socket getRawSocket() { return mySock; }

    public synchronized int getCurrentCommID() { return currentCommID; }

    public HWTCPServer getMaster() { return master; }

    public synchronized Optional<HWUser> getUser() { return Optional.ofNullable(myUser); }

    public synchronized void setUser(HWUser u) { myUser = u; }

    /**
     * This should repeatedly be called by the thread dedicated to the Handler
     * This retrieves messages, processes them and schedules lookups and calls to the ServerWorker
     *
     * @return Whether or the the thread can be terminated (or: Whether or not an error so critical occurred
     * that the connection can no longer be maintained by the Handler)
     */
    public boolean runAction() {

        try {

            if (!greeted) {

                greet();
                greeted = true;

            }

            if (mySock.isClosed()) {
                killConnection();
                return terminated;
            }

            String message = reader.readLine();

            if (message != null) {

                if (!message.startsWith("protoInfo")) {

                    JSONObject object = new JSONObject(message);

                    processJSON(object);

                } else {

                    sendProtocolInfo();
                    return terminated;
                }

            }

        } catch (SSLException sslEx) {

            if (!terminated) {
                master.sendLog(this, Level.FINEST, "Closing connection: " + sslEx.toString());
                killConnection();
            }
            return terminated;

        } catch (IOException ex) {

            if (!isClosed) {
                master.sendLog(this, Level.WARNING, "Unable to read socket: " + ex.toString());
            }
            killConnection();

        }  catch (JSONException ex) {

            JSONObject response = new JSONObject();

            response.put("type", "error");
            response.put("status", Status.BADREQUEST);
            response.put("status_message", Status.SBADREQUEST);
            response.put("error", "JSONException");
            response.put("error_message", ex.toString());

            sendJSON(response);

        }

        return terminated;

    }

    public synchronized void sendJSON(JSONObject json) {

        json.put("handler", currentCommHandler.getIdentifier());

        String message = json.toString().replaceAll("\n", "") + "\n";

        sendMessage(message);

    }

    /**
     * Sends a message through the socket
     * Remember that this does neither append \r nor \n
     *
     * @param message the message to be send
     */
    public synchronized void sendMessage(String message) {

        try {

            writer.write(message);
            writer.flush();

        } catch (IOException ex) {

            if (!(ex instanceof SSLException)) {

                master.sendLog(this, Level.WARNING, "Unable to send Message: " + ex.toString());
                killConnection();

            }
        }
    }

    private void greet() {

        //BEGIN PROTOCOL NOT FULLY MET
        JSONObject response = new JSONObject();

        response.put("status", Status.NOTIFY_DEV);
        response.put("payload_type", "message");

        JSONObject message = new JSONObject();
            message.put("type", "message");
            message.put("message", "This server is currently NOT meeting the full requirements of the hw protocol!");
            message.put("messagetype", "devinfo");


        response.put("payload", message);
        response.put("commID", 1);

        sendJSON(response);
        //END PROTOCOL NOT FULLY MET

        //BEGIN GREETING
        JSONObject jObj = new JSONObject();

        jObj.put("status", Status.OK);
        jObj.put("payload_type", "null");
        jObj.put("commID", 1);

        sendJSON(jObj);
        //END GREETING

    }

    private void sendProtocolInfo() {

        JSONObject response = new JSONObject();
        response.put("protoVersion", Status.SCURRENTPROTOVERSION);

        sendJSON(response);

    }

    private void killConnection() {

        terminated = true;

    }

    private void sendState_processing() {

        JSONObject response = Status.state_PROCESSING();

        response.put("commID", currentCommID);

        sendJSON(response);

    }

    private boolean require(JSONObject request, String field) {

        if (request.has(field)) {
            return true;
        }

        JSONObject response = Status.state_ERROR(
                Status.BADREQUEST,
                Status.state_genError(
                        "ProtocolError",
                        "Request is missing field \"" + field + "\"!",
                        "Request was incomplete, contact your client developer"
                ));

        response.put("commID", negateInt(currentCommID));

        sendJSON(response);

        return false;
    }

    private synchronized boolean requireUser() {
        if (myUser == null) {

            JSONObject response = Status.state_ERROR(
                    Status.UNAUTHORIZED,
                    Status.state_genError(
                            "PermissionError",
                            "Login request was missing or failed previously",
                            "Please log in first"
                    )
                );

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return false;
        }
        return true;
    }

    private void processJSON(JSONObject json) {

        if (!require(json, "command")) {
            return;
        }

        try {

            if (json.has("commID")) {

                currentCommID = json.getInt("commID");

            } else {

                L4YGRandom.initRndIfNotAlready();
                currentCommID = L4YGRandom.random.nextInt(5000) + 20;
                //Send the client the generated commID
                sendState_processing();

            }

            String command = json.getString("command");

             ArrayList<ICommandHandler> handlers = master.getMaster().getCommandHandlerProvider().getByCommand(command);

            if (handlers.size() == 0) {

                JSONObject response = Status.state_ERROR(
                        Status.BADREQUEST,
                        Status.state_genError(
                                "UnknownCommandError",
                                "Command \"" + command + "\" does not exist",
                                "Client request included an unknown command"
                        )
                );

                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

            } else {

                HWClientCommandContext c = new HWClientCommandContext(json, myReference);

                for (ICommandHandler h : handlers) {

                    currentCommHandler = h;
                    h.onMessage(c);

                }

                currentCommHandler = HANDLER_NATIVE;

                JSONObject r = Status.state_OK();

                r.put("commID", negateInt(currentCommID));

                sendJSON(r);

            }

        } catch (JSONException ex) {

            JSONObject response = Status.state_INTERNALEXCEPTION(ex);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

        } catch (Exception ex) {

            JSONObject response = Status.state_INTERNALEXCEPTION(ex);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            throw ex;

        }

    }

    public void closeConnection() {

        isClosed = true;

        try {

            mySock.close();

        } catch (IOException ex) {

            master.sendLog(this, Level.WARNING, "Unable to close connection: " + ex.toString());

        }
    }

}
