package de.mlessmann.config;

import de.mlessmann.hwserver.HWServer;

import java.io.*;
import java.util.logging.Logger;
import org.json.*;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class HWConfig {

    public static final String confVersion = "0.0.0.1";

    private final HWServer HWSERVER;
    private final Logger LOG;
    private String fileName;
    private boolean initialized = false;
    private JSONObject configObject;

    /**
     * Creates a new ConfigInstance
     * @param serverInstance Reference to the HWServer instance (mainly to retrieve the Logger)
     */
    public HWConfig(HWServer serverInstance) {

        if (serverInstance == null) {
            throw new IllegalArgumentException("serverInstance must not be null!");
        }

        this.HWSERVER = serverInstance;
        this.LOG = HWSERVER.getLogger();

    }

    /**
     * Open a file and read it as a config, check
     * @see #isInitialized for success
     * @param file absolute or relative path to the config
     * @return this
     */
    public HWConfig open(String file) {

        if (file == null) {
            throw new IllegalArgumentException("file must not be null!");
        }
        initialized = false;

        BufferedReader buff;
        StringBuilder content;

        try (FileReader fReader = new FileReader(file)) {
            //Read the file

            buff = new BufferedReader(fReader);

            content = new StringBuilder();

            buff.lines().forEach(content::append);

        } catch (FileNotFoundException ex) {

            LOG.warning("Unable to open file: " + file + ": " + ex.toString());
            return this;

        } catch (IOException ex) {

            LOG.warning("Unable to read file: " + file + ": " + ex.toString());
            return this;

        }

        String stringContent = content.toString();


        JSONObject json;
        try {
            json = new JSONObject(stringContent);

        } catch (JSONException ex) {

            //TODO: JSONException returns wrong position
            LOG.warning(
                    (new StringBuilder("Error reading file: ")
                            .append(file)
                            .append(": ")
                            .append(ex))
                            .toString());

            return this;
        }

        configObject = json;
        initialized = true;

        if (json.has("configVersion")) {
            LOG.fine(
                    new StringBuilder("Loaded config: ")
                            .append(file)
                            .append(" version ")
                            .append(json.get("configVersion"))
                            .toString());
        } else {
            LOG.info("Config " + file + " has no valid version annotation!");
        }

        return this;
    }

    public HWConfig createIfNotFound(String file) throws IOException {

        File fil  = new File(file);

        if (!fil.isFile()) {

            fil.createNewFile();

            LOG.fine("Creating default config in: " + fil.getAbsolutePath());

        } else {

            return this;

        }

        configObject = new JSONObject();

        configObject.put("type", "config");

        configObject.put("configVersion", confVersion);

        JSONArray groups = new JSONArray();

        groups.put("default");

        configObject.put("groups", groups);

        try (FileWriter writer = new FileWriter(file)) {

            writer.write(configObject.toString(2));

        }

        return this;

    }

    /**
     * Did the last #open() call succeed
     */
    public boolean isInitialized() {

        return initialized;

    }

    /**
     * Make the JSONObject accessible as long as there're no getters for config values
     * TODO: Implement abstract getters to config values
     * !DO NOT STORE THIS, RELOADS WONT BE
     */
    public JSONObject getJSON() {

        return configObject;

    }

}
