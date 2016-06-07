package de.mlessmann.network;

import de.mlessmann.homework.HWGroup;
import de.mlessmann.homework.HomeWork;
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
import java.util.logging.Level;

/**
 * Created by mark332 on 08.05.2016.
 * @author Life4YourGames
 */
public class HWTCPClientHandler {

    private HWTCPServer master;
    private Socket mySock;
    private BufferedReader reader;
    private BufferedWriter writer;
    private HWGroup myGroup;
    private boolean terminated = false;
    private boolean isClosed = false;
    private boolean greeted = false;

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

                JSONObject object = new JSONObject(message);

                processJSON(object);

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
        response.put("status_message", Status.SNOTIFY_DEV);
        response.put("notify", "This server is currently NOT meeting the full requirements of the hw protocol!");

        sendJSON(response);
        //END PROTOCOL NOT FULLY MET

        //BEGIN GREETING
        JSONObject jObj = new JSONObject();

        jObj.put("status", Status.OK);
        jObj.put("status_message", Status.SOK);

        sendJSON(jObj);
        //END GREETING

    }

    private void killConnection() {

        terminated = true;

    }

    private void sendState_processing() {

        JSONObject response = new JSONObject();

        response.put("status", Status.PROCESSING);
        response.put("status_message", Status.SPROCESSING);

        sendJSON(response);

    }

    private boolean require(JSONObject request, String field) {

        if (request.has(field)) {
            return true;
        }

        JSONObject response = new JSONObject();
        response.put("type", "error");
        response.put("status", Status.BADREQUEST);
        response.put("status_message", Status.SBADREQUEST);
        response.put("error", "ProtocolError");
        response.put("error_message", "Request is missing field \"" + field + "\"!");

        sendJSON(response);

        return false;
    }

    private boolean requireGroup() {
        if (myGroup == null) {

            JSONObject response = new JSONObject();

            response.put("status", Status.UNAUTHORIZED);
            response.put("status_message", Status.SUNAUTHORIZED);

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
            response.put("status_message", "Command " + command + " not found!");

            sendJSON(response);

        } catch (JSONException ex) {

            JSONObject response = new JSONObject();

            response.put("type", "error");
            response.put("status", Status.INTERNALERROR);
            response.put("status_message", Status.SINTERNALERROR);
            response.put("error", "JSONException");
            response.put("error_message", ex.toString());

            sendJSON(response);

        } catch (Exception ex) {

            JSONObject response = new JSONObject();

            response.put("status", Status.INTERNALERROR);
            response.put("status_message", Status.SINTERNALERROR);

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

        String group = request.getJSONArray("parameters").getString(0);

        Optional<HWGroup> hwGroup = master.getMaster().getGroup(group);

        if (!hwGroup.isPresent()) {

            myGroup = null;

            JSONObject response = new JSONObject();
            response.put("status", Status.NOTFOUND);
            response.put("status_message", "Group " + group + " wasn't found");

            sendJSON(response);
            return;
        }

        myGroup = hwGroup.get();

        JSONObject response = new JSONObject();
        response.put("status", Status.OK);
        response.put("status_message", Status.SOK);

        sendJSON(response);

    }

    private void performAddHW(JSONObject request) {

        if (!require(request, "homework")) {
            return;
        }

        if (!requireGroup()) {
            return;
        }

        JSONObject hwObj = request.getJSONObject("homework");

        sendState_processing();

        if (!HomeWork.checkValidity(hwObj)) {

            JSONObject response = new JSONObject();

            response.put("type", "error");
            response.put("error", "PutHWError");
            response.put("error_message", "Submitted HomeWork was invalid");
            response.put("status", Status.BADREQUEST);
            response.put("status_message", Status.SBADREQUEST);

            sendJSON(response);

            return;

        }

        boolean success = false;

        success = myGroup.addHW(hwObj);

        if (!success) {

            JSONObject response = new JSONObject();

            response.put("type", "error");
            response.put("error", "PutHWError");
            response.put("error_message", "HomeWork was not added");
            response.put("status", Status.INTERNALERROR);
            response.put("status_message", Status.SINTERNALERROR);

            sendJSON(response);

            return;

        } else {

            JSONObject response = new JSONObject();

            response.put("status", Status.CREATED);
            response.put("status_message", Status.SCREATED);

            sendJSON(response);

            return;
        }

    }

    private void performGetHW(JSONObject request) {

        JSONArray subjects = null;
        if (request.has("subjects")) {
            subjects = request.getJSONArray("subjects");
        }

        if (!request.has("date")) {

            if (!require(request, "fromdate")) {
                return;
            }
            if (!require(request, "todate")) {
                return;
            }

            if (!requireGroup()) {
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
                    subjects.forEach(s -> {
                        if (s instanceof String) {
                            subjectFilter.add((String) s);
                        }
                    });
                }

                ArrayList<HomeWork> hws = myGroup.getHWBetween(dateFrom, dateTo, subjectFilter, false);

                JSONObject response = new JSONObject();

                response.put("status", Status.OK);
                response.put("status_message", Status.SOK);

                JSONArray arr = new JSONArray();

                if (!request.has("cap")) {

                    hws.stream().forEach(hw -> arr.put(hw.getJSON()));

                } else {

                    if (request.getString("cap").equals("short")) {

                        hws.stream().forEach(hw -> arr.put(hw.getShort()));

                    } else if (request.getString("cap").equals("long")) {

                        hws.stream().forEach(hw -> arr.put(hw.getLong()));

                    }

                }
                response.put("type", "hw_array");
                response.put("payload", arr);

                sendJSON(response);

            } catch (JSONException ex) {

                JSONObject response = new JSONObject();

                response.put("type", "error");
                response.put("status", Status.BADREQUEST);
                response.put("status_message", Status.BADREQUEST);
                response.put("error", "JSONException");
                response.put("error_message", ex.toString());

                sendJSON(response);

            } catch (DateTimeException ex) {

                JSONObject response = new JSONObject();

                response.put("type", "error");
                response.put("status", Status.BADREQUEST);
                response.put("status_message", Status.SBADREQUEST);
                response.put("error", "DateTimeException");
                response.put("error_message", ex.toString());

                sendJSON(response);

            }

        } else {

            try {

                JSONArray datArr = request.getJSONArray("date");

                LocalDate date = LocalDate.of(datArr.getInt(0), datArr.getInt(1), datArr.getInt(2));

                sendState_processing();

                ArrayList<String> subjectFilter = null;
                if (subjects != null && subjects.length() > 0) {
                    subjects.forEach(s -> {
                        if (s instanceof String) {
                            subjectFilter.add((String) s);
                        }
                    });
                }

                ArrayList<HomeWork> hws = myGroup.getHWOn(date, subjectFilter);

                JSONObject response = new JSONObject();

                response.put("status", Status.OK);
                response.put("status_message", Status.SOK);

                JSONArray arr = new JSONArray();

                hws.stream().forEach(hw -> arr.put(hw.getJSON()));

                response.put("type", "hw_array");
                response.put("payload", arr);

                sendJSON(response);

            } catch (JSONException ex) {

                JSONObject response = new JSONObject();

                response.put("type", "error");
                response.put("status", Status.BADREQUEST);
                response.put("status_message", Status.BADREQUEST);
                response.put("error", "JSONException");
                response.put("error_message", ex.toString());

                sendJSON(response);

            } catch (DateTimeException ex) {

                JSONObject response = new JSONObject();

                response.put("type", "error");
                response.put("status", Status.BADREQUEST);
                response.put("status_message", Status.SBADREQUEST);
                response.put("error", "DateTimeException");
                response.put("error_message", ex.toString());

                sendJSON(response);

            }

        }

    }

    private void performDelHW(JSONObject request) {

        if (!require(request, "date") | !require(request, "id") | !requireGroup()) {

            return;

        }

        try {

            JSONArray date = request.getJSONArray("date");

            String id = request.getString("id");

            LocalDate ldate = LocalDate.of(date.getInt(0), date.getInt(1), date.getInt(2));

            boolean success = myGroup.delHW(ldate, id);

            if (!success) {

                JSONObject response = new JSONObject();

                response.put("type", "error");
                response.put("error", "DelHWError");
                response.put("error_message", "HomeWork could not be deleted");
                response.put("status", Status.INTERNALERROR);
                response.put("status_message", Status.SINTERNALERROR);

                sendJSON(response);

                return;

            } else {

                JSONObject response = new JSONObject();

                response.put("status", Status.CREATED);
                response.put("status_message", Status.SCREATED);

                sendJSON(response);

                return;

            }

        } catch (JSONException ex) {

            JSONObject response = new JSONObject();

            response.put("type", "error");
            response.put("status", Status.BADREQUEST);
            response.put("status_message", Status.BADREQUEST);
            response.put("error", "JSONException");
            response.put("error_message", ex.toString());

            sendJSON(response);

        } catch (DateTimeException ex) {

            JSONObject response = new JSONObject();

            response.put("type", "error");
            response.put("status", Status.BADREQUEST);
            response.put("status_message", Status.SBADREQUEST);
            response.put("error", "DateTimeException");
            response.put("error_message", ex.toString());

            sendJSON(response);

        }

    }

}
