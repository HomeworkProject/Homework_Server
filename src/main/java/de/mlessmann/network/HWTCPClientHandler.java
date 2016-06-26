package de.mlessmann.network;

import de.mlessmann.allocation.HWGroup;
import de.mlessmann.allocation.HWUser;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.perms.Permission;
import de.mlessmann.util.L4YGRandom;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.Socket;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;

/**
 * Created by MarkL4YG on 08.05.2016.
 * @author MarkL4YG
 * (C 2016) Magnus Leßmann
 */
public class HWTCPClientHandler {

    private static int negateInt(int i) {

        return i * ( -1 );

    }


    private HWTCPServer master;
    private Socket mySock;
    private BufferedReader reader;
    private BufferedWriter writer;
    private HWUser myUser;
    private boolean terminated = false;
    private boolean isClosed = false;
    private boolean greeted = false;
    private int currentCommID;

    public HWTCPClientHandler(Socket clientSock, HWTCPServer tcpServer) {

        this.mySock = clientSock;
        this.master = tcpServer;

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

    private void sendJSON(JSONObject json) {

        String message = json.toString().replaceAll("\n", "") + "\n";

        sendMessage(message);

    }

    /**
     * Sends a message through the socket
     * Remember that this does neither append \r nor \n
     *
     * @param message the message to be send
     */
    private void sendMessage(String message) {

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
        jObj.put("commID", -1);

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

        JSONObject response = new JSONObject();

        response.put("status", Status.PROCESSING);
        response.put("payload_type", "null");
        response.put("commID", currentCommID);

        sendJSON(response);

    }

    private boolean require(JSONObject request, String field) {

        if (request.has(field)) {
            return true;
        }

        JSONObject response = new JSONObject();
        response.put("status", Status.BADREQUEST);
        response.put("payload_type", "error");

        JSONObject e = new JSONObject();

            e.put("error", "ProtocolError");
            e.put("error_message", "Request is missing field \"" + field + "\"!");
            e.put("friendly_message", "Request was incomplete, contact your client developer");
        response.put("payload", e);

        response.put("commID", negateInt(currentCommID));

        sendJSON(response);

        return false;
    }

    private boolean requireUser() {
        if (myUser == null) {

            //TODO: Continue response refactoring!
            JSONObject response = new JSONObject();

            response.put("status", Status.UNAUTHORIZED);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();

                e.put("error", "PermissionError");
                e.put("error_message", "Login request was missing or failed previously");
                e.put("friendly_message", "Please log in first");
            response.put("payload", e);

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

            }

            String command = json.getString("command");

            if (command.equals("setgroup")) {
                performSetGroup(json);
                return;
            }
            if (command.equals("addhw")) {
                performAddHW(json);
                return;
            }
            if (command.equals("gethw")) {
                performGetHW(json);
                return;
            }
            if (command.equals("delhw")) {
                performDelHW(json);
                return;
            }

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();

                e.put("error", "ProtocolError");
                e.put("error_message", "Command \"" + command + "\" does not exist");
                e.put("friendly_message", "Client request included an unknown command");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));
            sendJSON(response);

        } catch (JSONException ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.INTERNALERROR);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "JSONError");
                e.put("error_message", ex.toString());
                e.put("friendly_message", "Internal server error occurred");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

        } catch (Exception ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.INTERNALERROR);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "Exception");
                e.put("error_message", ex.toString());
                e.put("friendly_message", "Internal server error occurred");
            response.put("payload", e);

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

    //----------BEGIN COMMAND IMPLEMENTATION-------------

    private void performSetGroup(JSONObject request) {

        if (!require(request, "parameters")) {

            return;

        }

        JSONArray arr = request.getJSONArray("parameters");

        String group = null;
        String user = null;
        String auth = null;

        if (arr.length() >= 1) {

            group = arr.getString(0);

        } else {

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "ProtocolError");
                e.put("error_message", "SetGroup needs at least 1 parameter");
                e.put("friendly_message", "Client request was incomplete");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);
            return;

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

        Optional<HWGroup> hwGroup = master.getMaster().getGroup(group);

        if (!hwGroup.isPresent()) {

            myUser = null;

            JSONObject response = new JSONObject();
            response.put("status", Status.NOTFOUND);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "NotFoundError");
                e.put("error_message", "Group " + group + " wasn't found");
                e.put("friendly_message", "Group \"" + group + "\" does not exist");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);
            return;

        }

        Optional<HWUser> hwUser = hwGroup.get().getUser(user, auth);

        if (!hwUser.isPresent()) {

            myUser = null;

            JSONObject response = new JSONObject();
            response.put("status", Status.NOTFOUND);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "NotFoundNumber");
                e.put("error_message", "User \"" + user + "\" wasn't found");
                e.put("friendly_message", "User \"" + user + "\" does not exist");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);
            return;

        }

        myUser = hwUser.get();

        JSONObject response = new JSONObject();
        response.put("status", Status.OK);
        response.put("payload_type", "null");
        response.put("commID", negateInt(currentCommID));

        sendJSON(response);

    }

    private void performAddHW(JSONObject request) {

        if (!require(request, "homework")) {
            return;
        }

        if (!requireUser()) {
            return;
        }

        JSONObject hwObj = request.getJSONObject("homework");

        sendState_processing();

        if (!HomeWork.checkValidity(hwObj)) {

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "PutHWError");
                e.put("error_message", "Submitted HomeWork was invalid");
                e.put("friendly_message", "Client submitted an invalid object");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return;

        }

        int success = myUser.addHW(hwObj);

        if (success == -1) {

            JSONObject response = new JSONObject();

            response.put("status", Status.INTERNALERROR);
            response.put("type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "PutHWError");
                e.put("error_message", "HomeWork not added");
                e.put("friendly_message", "HomeWork wasn't added due to a server error");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return;

        } else if (success == 0) {

            JSONObject response = new JSONObject();

            response.put("status", Status.CREATED);
            response.put("payload_type", "null");
            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return;

        } else if (success == 1) {

            JSONObject response = new JSONObject();

            response.put("status", Status.FORBIDDEN);
            response.put("type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "InsufficientPermissionError");
                e.put("error_message", "Insufficient permission to add the homework");
                e.put("friendly_message", "You don't have enough permission to add a homework");
                e.put("perm", "has:" + Permission.HW_ADD_NEW);
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return;

        } else if (success == 2) {

            JSONObject response = new JSONObject();

            response.put("status", Status.FORBIDDEN);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "InsufficientPermissionError");
                e.put("error_message", "Insufficient permission to edit the homework");
                e.put("friendly_message", "You don't have enough permission to edit this homework");
                e.put("perm", "has:" + Permission.HW_ADD_EDIT);
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return;

        }

    }

    private void performGetHW(JSONObject request) {

        JSONArray subjects = null;
        if (request.has("subjects")) {
            subjects = request.getJSONArray("subjects");
        }

        if (!requireUser()) {
            return;
        }

        if (!request.has("date")) {

            if (!require(request, "fromdate")) {
                return;
            }
            if (!require(request, "todate")) {
                return;
            }

            try {

                JSONArray fromDate = request.getJSONArray("fromdate");

                JSONArray toDate = request.getJSONArray("todate");

                int fyyyy = fromDate.getInt(0);
                int fMM = fromDate.getInt(1);
                int fdd = fromDate.getInt(2);

                int tyyyy = toDate.getInt(0);
                int tMM = toDate.getInt(1);
                int tdd = toDate.getInt(2);

                sendState_processing();

                LocalDate dateFrom = LocalDate.of(fyyyy, fMM, fdd);
                LocalDate dateTo = LocalDate.of(tyyyy, tMM, tdd);

                ArrayList<String> subjectFilter = null;
                if (subjects != null && subjects.length() > 0) {

                    subjectFilter = new ArrayList<String>();

                    ArrayList<String> finalSubjectFilter = subjectFilter;

                    subjects.forEach(s -> {
                        if (s instanceof String) {
                            finalSubjectFilter.add((String) s);
                        }
                    }
                    );
                }

                ArrayList<HomeWork> hws = myUser.getHWBetween(dateFrom, dateTo, subjectFilter, false);

                JSONObject response = new JSONObject();

                response.put("status", Status.OK);
                response.put("status_message", Status.SOK);

                JSONArray arr = new JSONArray();

                if (!request.has("cap")) {

                    hws.stream().forEach(hw -> arr.put(hw.getJSON()));

                } else {

                    if (request.getString("cap").equals("short")) {

                        hws.stream().forEach(hw -> arr.put(new JSONObject().put("short", hw.getShort())));

                    } else if (request.getString("cap").equals("long")) {

                        hws.stream().forEach(hw -> arr.put(new JSONObject().put("long", hw.getLong())));

                    }

                }
                response.put("payload_type", "JSONArray");
                response.put("array_type", "HWObject");
                response.put("payload", arr);
                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

            } catch (JSONException ex) {

                JSONObject response = new JSONObject();

                response.put("status", Status.BADREQUEST);
                response.put("payload_type", "error");

                JSONObject e = new JSONObject();
                    e.put("error", "JSONError");
                    e.put("error_message", ex.toString());
                    e.put("friendly_message", "Client sent an invalid request");
                response.put("payload", e);

                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

            } catch (DateTimeException ex) {

                JSONObject response = new JSONObject();

                response.put("status", Status.BADREQUEST);
                response.put("payload_type", "error");

                JSONObject e = new JSONObject();
                    e.put("error", "DateTimeException");
                    e.put("error_message", ex.toString());
                    e.put("friendly_message", "Client sent an invalid request");
                response.put("payload", e);

                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

            }

        } else {

            try {

                JSONArray datArr = request.getJSONArray("date");

                LocalDate date = LocalDate.of(datArr.getInt(0), datArr.getInt(1), datArr.getInt(2));

                sendState_processing();

                ArrayList<String> subjectFilter = null;
                if (subjects != null && subjects.length() > 0) {

                    subjectFilter = new ArrayList<String>();

                    ArrayList<String> finalSubjectFilter = subjectFilter;

                    subjects.forEach(s -> {
                        if (s instanceof String) {
                            finalSubjectFilter.add((String) s);
                        }
                    });
                }

                ArrayList<HomeWork> hws = myUser.getHWOn(date, subjectFilter);

                JSONObject response = new JSONObject();

                response.put("status", Status.OK);

                JSONArray arr = new JSONArray();

                hws.stream().forEach(hw -> arr.put(hw.getJSON()));

                response.put("payload_type", "JSONArray");
                response.put("array_type", "HWObject");
                response.put("payload", arr);
                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

            } catch (JSONException ex) {

                JSONObject response = new JSONObject();

                response.put("status", Status.BADREQUEST);
                response.put("payload_type", "error");

                JSONObject e = new JSONObject();
                    e.put("error", "JSONException");
                    e.put("error_message", ex.toString());
                    e.put("friendly_message", "Client sent an invalid request");
                response.put("payload", e);

                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

            } catch (DateTimeException ex) {

                JSONObject response = new JSONObject();

                response.put("status", Status.BADREQUEST);
                response.put("payload_type", "error");

                JSONObject e = new JSONObject();
                    e.put("error", "DateTimeException");
                    e.put("error_message", ex.toString());
                    e.put("friendly_message", "Client sent an invalid request");
                response.put("payload", e);

                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

            }

        }

    }

    private void performDelHW(JSONObject request) {

        if (!require(request, "date") | !require(request, "id") | !requireUser()) {

            return;

        }

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

                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

            } else if (success == 0) {

                JSONObject response = new JSONObject();

                response.put("status", Status.OK);
                response.put("payload_type", "null");
                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

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

                response.put("commID", negateInt(currentCommID));

                sendJSON(response);

                return;

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

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return;

        } catch (DateTimeException ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.BADREQUEST);
            response.put("payload_type", "error");

            JSONObject e = new JSONObject();
                e.put("error", "DateTimeException");
                e.put("error_message", ex.toString());
                e.put("status_message", "Client sent an invalid request");
            response.put("payload", e);

            response.put("commID", negateInt(currentCommID));

            sendJSON(response);

            return;

        }

    }

}
