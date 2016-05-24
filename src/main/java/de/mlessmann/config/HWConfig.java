package de.mlessmann.config;

import de.mlessmann.hwserver.HWServer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;
import org.json.*;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class HWConfig {

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
