package de.mlessmann.config;

import de.mlessmann.hwserver.HWServer;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import java.util.logging.Logger;
import org.json.*;

/**
 * Created by Life4YourGames on 29.04.16.
 */
public class HWConfig {

    private final HWServer HWSERVER;
    private final Logger LOG;
    private String fileName;
    private Boolean initialized = false;
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

        Scanner scanner = null;
        FileReader reader;
        StringBuilder content;

        try {
            //Read the file
            reader = new FileReader(file);

            scanner = new Scanner(reader);

            scanner.useDelimiter("[\\n]");

            content = new StringBuilder();
            scanner.forEachRemaining(s -> content.append(s));

        } catch (FileNotFoundException ex) {

            LOG.warning("Unable to open file: " + file + ": " + ex.toString());
            return this;

        } /* Would catch any other IOException but IntelliJ cries if I try to ._.
            catch (IOException ex) {

            LOG.warning("Unable to read file: " + file + ": " + ex.toString());
            return this;

        } */
            finally {
            if (scanner != null)
                scanner.close();
        }

        String stringContent = content.toString();


        JSONObject json = null;
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
    public Boolean isInitialized() {

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
