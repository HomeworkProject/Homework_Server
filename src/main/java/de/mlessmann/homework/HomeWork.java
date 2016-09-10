package de.mlessmann.homework;

import de.mlessmann.hwserver.HWServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

/**
 * Created by Life4YourGames on 04.05.16.
 * @author Life4YourGamesk
 */
public class HomeWork {

    private JSONObject contentAsJSON = new JSONObject();
    private File file;
    private String filePath;
    private boolean isLoaded = false;
    public boolean flushInstant = false;
    private HWServer master;

    public HomeWork(HWServer server) {

        super();
        master = server;

    }

    public HomeWork(String path, HWServer server) {

        this(server);
        setPath(path);

    }

    public HomeWork(JSONObject json, HWServer server) {

        this(server);
        setJSON(json);

    }

    public static HomeWork newByPath(String path, HWServer master) {

        return new HomeWork(path, master);

    }

    public HomeWork setPath(String path) {

        filePath = path;
        isLoaded = false;

        return this;
    }

    public HomeWork setJSON(JSONObject nJSON) {

        contentAsJSON = nJSON;

        notifyOfChange();

        return this;

    }

    private boolean initializeFile(boolean createFile) {

        file = new File(filePath);

        try {

            if (!file.isFile()) {

                if (!createFile) {
                    return false;
                }

                if (!file.createNewFile()) {

                    master.getLogger().warning("Unable to initialize HomeWork-file: File#CreateNewFile returned false");

                    return false;

                }

            }

        } catch (IOException ex) {

            master.getLogger().warning("Unable to initialize HomeWork-file: " + ex.toString());

            return false;

        }

        return file.canRead();

    }

    public boolean flush() {

        if (!initializeFile(true)) {

            return false;

        }

        try (FileWriter writer = new FileWriter(file)){

            writer.write(contentAsJSON.toString(2));

        } catch (IOException ex){

            master.getLogger().warning("Unable to flush HomeWork: " + ex.toString());

            return false;

        }

        return true;

    }

    public boolean read() {

        master.getLogger().finest("HW#READ: " + filePath);

        boolean skip = false;

        if (!initializeFile(false)) {

            skip = true;

        }

        if (!skip) {

            StringBuilder content = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

                reader.lines().forEach(content::append);

                JSONObject jObj = new JSONObject(content.toString());

                boolean isValid = false;


                if (checkValidity(jObj)) {

                    contentAsJSON = jObj;

                    if (!contentAsJSON.has("subject")) {

                        contentAsJSON.put("subject", "none");

                    }

                    isLoaded = true;

                    return true;

                } else {

                    master.getLogger().warning("Unable to read HomeWork \"" + filePath + "\": Invalid HomeWork");
                    isLoaded = true;

                }

            } catch (IOException ex) {

                master.getLogger().warning("Unable to read HomeWork \"" + filePath + "\": " + ex.toString());

            } catch (JSONException ex) {

                master.getLogger().warning("Unable to load HomeWork \"" + filePath + "\": " + ex.toString());

            }

        } else {
            master.getLogger().warning("Unable to load HomeWork \"" + filePath + "\": Initialization failed");
        }

        return false;

    }

    /**
     * This must be called after any change to the json, that hasn't been done by this class
     * (e.g. directly changing the jsonObject using #getJSON)
     * This is performing "instant"Flush if it's enabled
     * This is called automatically if you use #setJSON
     * This only concerns the JSONObject, neither the path nor anything else
     * @return this
     */
    public HomeWork notifyOfChange() {

        if (flushInstant) {

            flush();

        }

        return this;

    }

    public static boolean checkValidity(JSONObject any) {

        if (any == null) return false;

        boolean validity = false;

        try {

            validity = any.optString("type", "null").equalsIgnoreCase("HomeWork")
                    && (any.optString("title", null) != null)
                    && (any.optString("desc", null) != null);

            if (validity) {

                JSONArray date = any.getJSONArray("date");

                validity = date.getInt(0) > 0
                                    && date.getInt(1) > 0 && date.getInt(1) <= 12
                                    && date.getInt(2) > 0 && date.getInt(2) <= 31;

            }

        } catch (JSONException ex) {

            return false;

        }

        return validity;

    }

    @Override
    public String toString() {
        if (!isLoaded) read();
        return contentAsJSON.toString();
    }

    public String toString(int indent) {
        if (!isLoaded) read();
        return contentAsJSON.toString(indent);
    }

    public JSONObject getJSON() {
        if (!isLoaded) read();
        return contentAsJSON;
    }

    public JSONObject getLong() {
        if (!isLoaded) read();
        JSONObject res = null;

        try {

            res = contentAsJSON.getJSONObject("long");

        } catch (JSONException ex) {

        }

        if (res == null) {

            res = new JSONObject();

        }

        return res;

    }

    public JSONObject getShort() {
        if (!isLoaded) read();
        JSONObject res = null;

        try {

            res = contentAsJSON.getJSONObject("short");

        } catch (JSONException ex) {

        }

        if (res == null) {

            res = new JSONObject();

        }

        return res;

    }

    public boolean isValid() {
        return HomeWork.checkValidity(this.getJSON());
    }

}
