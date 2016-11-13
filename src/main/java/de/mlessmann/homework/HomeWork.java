package de.mlessmann.homework;

import de.mlessmann.common.L4YGRandom;
import de.mlessmann.hwserver.HWServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        file = new File(path);

        return this;
    }

    public HomeWork setJSON(JSONObject nJSON) {
        contentAsJSON = nJSON;
        notifyOfChange();
        return this;
    }

    private boolean initializeFile(boolean createFile) {

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

    public String getID() {
        String name = getFile().getName();
        return name.substring(0, name.indexOf(".json"));
    }

    public boolean isValid() {
        return HomeWork.checkValidity(this.getJSON());
    }

    public File getFile() { return file; }

    public String getNewAttachmentID() {
        String id = file.getName().substring(0, file.getName().indexOf("."));
        File f = new File(id);
        L4YGRandom.initRndIfNotAlready();
        if (f.isDirectory()) {
            String attachID;
            do {
                attachID = L4YGRandom.genRandomAlphaNumString(20);
            } while (new File(f, attachID).exists());
            return attachID;
        } else {
            L4YGRandom.initRndIfNotAlready();
            return L4YGRandom.genRandomAlphaNumString(20);
        }
    }

    /**
     * Registers a new attachment to the HomeWork
     * Remember that assetID and hwID will not be overwritten!
     * Thus you need to provide them!
     * @param attachment AttachmentLocation
     * @return Whether or not the registration was successful
     */
    public boolean registerAttachment(HWAttachmentLocation attachment) {
        JSONObject attachJSON = null;
        if (attachment.getType() == HWAttachmentLocation.LocationType.WEB) {
            attachJSON = new JSONObject();
            attachJSON.put("url", attachment.getURL());
        } else if (attachment.getType() == HWAttachmentLocation.LocationType.SERVER) {
            String hwID = attachment.getHWID();
            LocalDate date = attachment.getDate();
            String id = getNewAttachmentID();

            attachJSON = new JSONObject();
            JSONArray jArr = new JSONArray();
            jArr.put(date.getYear());
            jArr.put(date.getMonthValue());
            jArr.put(date.getDayOfMonth());
            attachJSON.put("date", jArr);
            attachJSON.put("ownerid", hwID);
            attachJSON.put("id", id);
        }

        if (attachJSON == null) return false;
        if (getJSON().optJSONArray("attachments") == null) getJSON().put("attachments", new JSONArray());
        getJSON().getJSONArray("attachments").put(attachJSON);
        return true;
    }

    public List<HWAttachment> getAttachments() {
        List<HWAttachment> l = new ArrayList<HWAttachment>();
        JSONArray attachments = getJSON().optJSONArray("attachments");
        attachments.forEach(o -> {
            if (o instanceof JSONObject) {
                l.add(new HWAttachment(((JSONObject) o), this));
            }
        });
        return l;
    }

    public Optional<HWAttachment> getAttachment(String id) {
        List<HWAttachment> l = getAttachments();
        HWAttachment[] a = new HWAttachment[]{null};
        l.forEach(b -> {
            if (b.getID().equals(id))
                a[0] = b;
        });
        return Optional.ofNullable(a[0]);
    }
}
