package de.mlessmann.network.commands;

import de.mlessmann.allocation.HWUser;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.network.HWClientCommandContext;
import de.mlessmann.network.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Created by Life4YourGames on 28.06.16.
 */

@HWCommandHandler
public class nativeCommGetHW extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.getHW";
    public static final String COMMAND = "gethw";

    public nativeCommGetHW() {

        setID(IDENTIFIER);
        setCommand(COMMAND);

    }

    @Override
    public boolean onMessage(HWClientCommandContext context) {

        JSONArray subjects = null;
        if (context.getRequest().has("subjects")) {
            subjects = context.getRequest().getJSONArray("subjects");
        }

        if (!requireUser(context.getHandler())) {
            return false;
        }


        Optional<HWUser> u = context.getHandler().getUser();
        //IsPresent checked in #requireUser(HWTCPClientReference) above
        //noinspection OptionalGetWithoutIsPresent
        HWUser myUser = u.get();

        if (!context.getRequest().has("date")) {

            if (!require(context.getRequest(), "fromdate", context.getHandler())) {
                return false;
            }
            if (!require(context.getRequest(), "todate", context.getHandler())) {
                return false;
            }

            try {

                JSONArray fromDate = context.getRequest().getJSONArray("fromdate");

                JSONArray toDate = context.getRequest().getJSONArray("todate");

                int fyyyy = fromDate.getInt(0);
                int fMM = fromDate.getInt(1);
                int fdd = fromDate.getInt(2);

                int tyyyy = toDate.getInt(0);
                int tMM = toDate.getInt(1);
                int tdd = toDate.getInt(2);

                JSONObject p = Status.state_PROCESSING();
                p.put("commID", context.getHandler().getCurrentCommID());
                sendJSON(context.getHandler(), p);

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

                if (!context.getRequest().has("cap")) {

                    hws.stream().forEach(hw -> arr.put(hw.getJSON()));

                } else {

                    if (context.getRequest().getString("cap").equals("short")) {

                        hws.stream().forEach(hw -> arr.put(new JSONObject().put("short", hw.getShort())));

                    } else if (context.getRequest().getString("cap").equals("long")) {

                        hws.stream().forEach(hw -> arr.put(new JSONObject().put("long", hw.getLong())));

                    }

                }
                response.put("payload_type", "JSONArray");
                response.put("array_type", "HWObject");
                response.put("payload", arr);
                response.put("commID", context.getHandler().getCurrentCommID());

                sendJSON(context.getHandler(), response);

                return true;

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
                e.put("friendly_message", "Client sent an invalid request");
                response.put("payload", e);

                response.put("commID", context.getHandler().getCurrentCommID());

                sendJSON(context.getHandler(), response);

                return false;

            }

        } else {

            try {

                JSONArray datArr = context.getRequest().getJSONArray("date");

                LocalDate date = LocalDate.of(datArr.getInt(0), datArr.getInt(1), datArr.getInt(2));

                JSONObject p = Status.state_PROCESSING();
                p.put("commID", context.getHandler().getCurrentCommID());
                sendJSON(context.getHandler(), p);

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
                response.put("commID", context.getHandler().getCurrentCommID());

                sendJSON(context.getHandler(), response);

                return true;

            } catch (JSONException ex) {

                JSONObject response = new JSONObject();

                response.put("status", Status.BADREQUEST);
                response.put("payload_type", "error");

                JSONObject e = new JSONObject();
                e.put("error", "JSONException");
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
                e.put("friendly_message", "Client sent an invalid request");
                response.put("payload", e);

                response.put("commID", context.getHandler().getCurrentCommID());

                sendJSON(context.getHandler(), response);

                return false;

            }

        }

    }

}
