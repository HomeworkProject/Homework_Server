package de.mlessmann.hwserver;

import de.mlessmann.config.HWConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.mlessmann.homework.HWGroup;
import de.mlessmann.logging.*;
import de.mlessmann.network.HWTCPServerWorker;
import org.json.JSONArray;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class HWServer {

    /**
     * TCPServerWorker used for TCPCommunication
     */
    private HWTCPServerWorker hwtcpServerWorker;

    /**
     * Logger used to write to console (stdOut)
     * @see #getLogger()
     */
    private final Logger LOG = Logger.getLogger("HW");
    /**
     * FileName of the log
     * @see #getLogFile()
     */
    private final String LOGFILE = "hwserver-latest.log";

    /**
     * Formatter used by the HWServer instance
     */
    private final HWLogFormatter LOGFORMATTER = new HWLogFormatter();

    /**
     * ConsoleHandler for logging to console->StdOut
     */
    private final HWConsoleHandler HCONSOLE = new HWConsoleHandler(System.out);

    /**
     * FileHandler for writing the "latest"-log
     */
    private FileHandler logFileHandler;

    /**
     * Path to the config
     * @see #setArg(String)
     */
    private String confFile = "conf/config.json";

    /**
     * Configuration object (using JSON)
     * @see #getConfig
     */
    private HWConfig config;

    /**
     * HashMap to store groups
     * @see #getGroup(String)
     */
    private Map<String, HWGroup> hwGroups = new HashMap<String, HWGroup>();

    /**
     * Create a new Server instance
     * Currently multiple instances are not natively implemented
     */
    public HWServer() {

    }

    /**
     * Run PreInitialization: You may not want to run this a second time
     * @return this
     */
    public HWServer preInitialize() throws IOException {

        logFileHandler = new FileHandler(getLogFile());
        logFileHandler.setFormatter(LOGFORMATTER);

        //Initialize LogLevels and debug
        LOGFORMATTER.setDebug(false);
        HCONSOLE.setLevel(Level.FINEST);
        logFileHandler.setLevel(Level.FINEST);
        LOG.setLevel(Level.FINEST);

        //Replace the default console formatter
        HCONSOLE.setFormatter(LOGFORMATTER);
        LOG.addHandler(HCONSOLE);
        LOG.addHandler(logFileHandler);

        LOG.fine("Entering preInitialization");

        //PreInit config so the reference is correct
        config = new HWConfig(this);

        return this;
    }

    /**
     * Run Initialization: You may not want to run this a second time
     * @return this
     */
    public HWServer initialize() throws IOException {

        LOG.fine("Entering initialization");

        File confDir = new File("conf");

        if (!confDir.isDirectory()) {

            if (!confDir.mkdir()) {

                LOG.severe("Unable to create dir \"conf\"!");

                throw new IOException("Cannot create directory: "+ confDir.getAbsolutePath());

            } else {

                LOG.fine("Created default conf dir");

            }

        }


        if (!config.createIfNotFound(confFile).open(confFile).isInitialized()) {
            LOG.severe("Unable to read config! This instance is not going to work!");
            throw new IOException("Config not readable");
        }

        JSONArray arr = config.getJSON().getJSONArray("groups");

        arr.forEach(s -> loadGroup(s.toString()));

        return this;
    }

    /**
     * Load files for a specific group
     */
    private void loadGroup(String gName) {

        HWGroup newGroup = new HWGroup(gName, this);

        newGroup.init();

        hwGroups.put(gName, newGroup);

    }

    /**
     * Start the server
     */

    public HWServer start() {

        hwtcpServerWorker = new HWTCPServerWorker(this);

        hwtcpServerWorker.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            try {

                String command = reader.readLine();

                if (command.startsWith("exit")) {

                    break;

                }

            } catch (IOException ex) {

                LOG.throwing(this.getClass().toString(), "start", ex);

            }
        }

        hwtcpServerWorker.stopServer();

        //hwGroups.forEach((k, v) -> v.flushToFiles());
        //HomeWorks are flushed on addition, this is currently not needed
        //However caching may come back-> This is a reminder

        return this;

    }

    /**
     * @param args This should be the start arguments (of the application)
     */
    public HWServer setArgs(String[] args) {
        if (args.length > 0) {
            Arrays.stream(args).forEach(this::setArg);
        }
        return this;
    }

    /**
     * Sets one specific argument. Remember: Args cannot be removed, just adjusted
     * @param arg The argument in for: <code>"argument=key"</code>
     * @return this
     */
    public HWServer setArg(String arg) {

        if (arg.contains("=")) {

            String key = arg.substring(0, arg.indexOf('='));
            String value = arg.substring(arg.indexOf('='));

            switch (key) {
                case "-config": confFile = value; break;
                default: LOG.warning("Unsupported argument: " + key);
            }

        } else {
            switch (arg) {
                case "-debug": enableDebug(); break;
                case "--log-no-trace": LOGFORMATTER.setDebug(false); break;
                default: LOG.warning("Unsupported argument: " + arg); break;
            }
        }

        return this;
    }

    /**
     * Enable debug mode for this instance
     * @return this
     */
    public HWServer enableDebug() {

        LOG.setLevel(Level.FINEST);
        LOGFORMATTER.setDebug(true);
        HCONSOLE.setLevel(Level.FINEST);

        LOG.fine("Debug mode enabled");

        return this;
    }

    /**
     * @return (HW) logger
    */
    public Logger getLogger() {
        return LOG;
    }

    /**
     * This returns the filename of the logFile
     * @return logFile name(rel or abs path as String)
     */
    public String getLogFile() {

        return LOGFILE;
    }

    /**
     * Returns the configuration reference of the server
     * May be null if called before initialisation
     */
    public HWConfig getConfig() {
        return config;
    }

    public synchronized Optional<HWGroup> getGroup(String group) {

        HWGroup hwGroup = null;

        if (hwGroups.containsKey(group)) {

            hwGroup = hwGroups.get(group);

        }

        return Optional.ofNullable(hwGroup);

    }

}
