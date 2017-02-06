package de.homeworkproject.server.config;

import de.homeworkproject.server.fileutil.FileUtil;
import de.homeworkproject.server.hwserver.HWServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
@Deprecated
public class HWConfig {

    public static final String confVersion = "0.0.0.1";

    private HWServer HWSERVER;
    private final Logger LOG;
    private String fileName;
    private FileUtil myFileUtil;
    private boolean initialized = false;
    private JSONObject configObject;
    public JSONObject defaultConf = new JSONObject();

    /**
     * Creates a new ConfigInstance
     * @param serverInstance Reference to the HWServer instance (mainly to retrieve the Logger)
     */
    public HWConfig(HWServer serverInstance) {

        if (serverInstance != null) {

            this.HWSERVER = serverInstance;
            this.LOG = HWSERVER.getLogger();

        } else {

            this.LOG = Logger.getGlobal();

        }

    }

    public HWConfig createIfNotFound(String file) {

        return createIfNotFound(new File(file));

    }

    public HWConfig createIfNotFound(File file) {

        if (myFileUtil == null || file != null) {
            myFileUtil = new FileUtil(file);
            myFileUtil.setLogger(LOG);
        }

        if (!myFileUtil.getFile().isFile()) {

            LOG.fine("Creating default config in: " + myFileUtil.getFile().getAbsolutePath());

            String confCachedWarning = "Unable to create default config: caching! The instance config will not be saved!";

            try {

                if (!myFileUtil.getFile().createNewFile()) {

                    LOG.warning(confCachedWarning);

                }

            } catch (IOException ex) {

                LOG.warning(confCachedWarning);

            }

        } else {

            return this;

        }

        configObject = defaultConf;

        myFileUtil.writeToFile(configObject.toString(2));

        return this;

    }

    public JSONObject getConfigObject() {
        return configObject;
    }

    /**
     * Open a file and read it as a config, check
     * @see #isInitialized for success
     * @param file absolute or relative path to the config
     * @return this
     */
    public HWConfig open(String file) {

        return open(new File(file));

    }

    /**
     * Open a file and read it as a config, check
     * @see #isInitialized for success
     * @param file object of the config
     * @return this
     */
    public HWConfig open(File file) {

        if (myFileUtil == null || file != null) {

            myFileUtil = new FileUtil(file);
            myFileUtil.setLogger(LOG);

        }

        initialized = false;

        StringBuilder content = myFileUtil.getContent(true);

        String stringContent = content.toString();


        JSONObject json;
        try {
            json = new JSONObject(stringContent);

        } catch (JSONException ex) {

            LOG.warning(
                    (new StringBuilder("Error reading file: ")
                            .append(myFileUtil.getFile().getPath())
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
                            .append(myFileUtil.getFile().getPath())
                            .append(" version ")
                            .append(json.get("configVersion"))
                            .toString());
        } else {
            LOG.info("Config " + file + " has no valid version annotation!");
        }

        return this;
    }

    public boolean flush() {

        return myFileUtil != null && myFileUtil.writeToFile(getJSON().toString(2));

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
