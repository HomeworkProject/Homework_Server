package de.mlessmann.hwserver;

import de.mlessmann.reflections.AuthLoader;
import de.mlessmann.reflections.AuthProvider;
import de.mlessmann.config.HWConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.mlessmann.allocation.HWGroup;
import de.mlessmann.logging.*;
import de.mlessmann.network.HWTCPServer;
import de.mlessmann.reflections.CommHandProvider;
import de.mlessmann.reflections.CommandLoader;
import de.mlessmann.updates.IAppRelease;
import de.mlessmann.updates.UpdateManager;
import de.mlessmann.util.apparguments.AppArgument;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLServerSocketFactory;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class HWServer {

    public static final String VERSION = "0.0.0.1";

    /**
     * Updater - well everyone knows what this is
     */
    private UpdateManager updater;
    private int updateMode = 1;
    //Collect start arguments to pass them through to the updater
    private ArrayList<AppArgument> startArgs = new ArrayList<AppArgument>();

    /**
     * TCPServerWorker used for TCPCommunication
     */
    private HWTCPServer hwtcpServer;

    /**
     * Logger used to write to console (stdOut)
     * @see #getLogger()
     */
    private final Logger LOG = Logger.getLogger("hwserver");
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
    private final HWConsoleHandler HCONSOLESTD = new HWConsoleHandler(System.out);
    private final HWConsoleHandler HCONSOLEERR = new HWConsoleHandler(System.err);

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
     * Provider for commandHandlers
     */
    private CommHandProvider myCommHandProvider;

    /**
     * Provider for Authentication Methods
     */
    private AuthProvider myAuthProvider;

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

        // -------------------------------- PRE INIT --------------------------------
        // -------------------------------- PRE INIT --------------------------------
        // -------------------------------- PRE INIT --------------------------------

        logFileHandler = new FileHandler(getLogFile());
        logFileHandler.setFormatter(LOGFORMATTER);

        //Initialize LogLevels and debug
        LOGFORMATTER.setDebug(false);

        HCONSOLESTD.setLevel(Level.FINEST);
        HCONSOLEERR.setLevel(Level.WARNING);

        logFileHandler.setLevel(Level.FINEST);

        LOG.setLevel(Level.FINEST);

        LOG.setUseParentHandlers(false);

        //Replace the default console formatter
        HCONSOLESTD.setFormatter(LOGFORMATTER);
        HCONSOLEERR.setFormatter(LOGFORMATTER);
        LOG.addHandler(HCONSOLESTD);
        LOG.addHandler(HCONSOLEERR);
        LOG.addHandler(logFileHandler);

        LOG.info("------Entering preInitialization------");

        //PreInit config so the reference is correct
        config = new HWConfig(this);

        JSONObject defaultConfig = new JSONObject();

        defaultConfig.put("type", "config");

        defaultConfig.put("configVersion", HWConfig.confVersion);

        JSONArray groups = new JSONArray();

        groups.put("default");

        defaultConfig.put("groups", groups);

        config.defaultConf = defaultConfig;

        return this;
    }

    /**
     * Run Initialization: You may not want to run this a second time
     * @return this
     */
    public HWServer initialize() throws IOException {

        // -------------------------------- INIT --------------------------------
        // -------------------------------- INIT --------------------------------
        // -------------------------------- INIT --------------------------------
        LOG.info("------Entering initialization------");

        // --- Command Handler ---

        LOG.fine("Trying to load CommandHandler");

        myCommHandProvider = new CommHandProvider();
        myCommHandProvider.setLogger(LOG);

        CommandLoader cLoader = new CommandLoader();

        cLoader.setProvider(myCommHandProvider);
        cLoader.setLogger(LOG);
        cLoader.setMaster(this);

        cLoader.loadAll();

        LOG.fine(myCommHandProvider.getHandler().size() + " handler registered.");

        // --- Auth Methods ---

        LOG.fine("Trying to load Authmethods");

        myAuthProvider = new AuthProvider();
        myAuthProvider.setLogger(LOG);

        AuthLoader aLoader = new AuthLoader();

        aLoader.setProvider(myAuthProvider);
        aLoader.setLogger(LOG);
        aLoader.setMaster(this);

        aLoader.loadAll();

        LOG.fine(myAuthProvider.getMethods().size() + " methods registered.");

        // --- Config ---

        LOG.fine("Trying to load configuration");

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

        File groupDir = new File("groups");

        if (groupDir.isFile()) {

            String msg = "File \"groups\" is occupying the groups directory! Delete or move the file before starting the server";

            LOG.severe(msg);
            throw new FileAlreadyExistsException(msg);

        }

        if (!groupDir.isDirectory() && !groupDir.mkdirs()) {

            String msg = "Unable to create group directory!";

            LOG.severe(msg);
            throw new IOException(msg);

        }


        JSONArray arr = config.getJSON().getJSONArray("groups");

        arr.forEach(s -> loadGroup(s.toString()));

        LOG.info(hwGroups.size() + " groups registered");

        // -------------------------------- POST INIT --------------------------------
        // -------------------------------- POST INIT --------------------------------
        // -------------------------------- POST INIT --------------------------------

        LOG.info("------Entering post-initialization------");

        updater = new UpdateManager();

        try {

            updater.setLogger(LOG);
            updater.setMode(updateMode);

            updater.setArgs(startArgs);

            updater.run();

            if (updater.isUpdateAvailable()) {

                IAppRelease r = updater.getLastResult().get();

                LOG.info("#########################################");
                LOG.info("There's an update available: " + r.getVersion());
                LOG.info("#########################################");

            } else {

                if (updater.getErrorCode() == 0)
                    LOG.info("Instance up to date! :)");
                else
                    LOG.warning("Unable to check for updates! Updater returned code: " + updater.getErrorCode());

            }

            LOG.info("------Exiting initialization phase------");

        } catch (Exception e) {

            LOG.severe("Unable to check for updates!");
            e.printStackTrace();

        }

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

        hwtcpServer= new HWTCPServer(this);

        if (!hwtcpServer.setUp()) {

            LOG.severe("An error occurred while setting up the tcp connections, this instance is going silent now!");
            return this;

        }

        hwtcpServer.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        loop: while (true) {

            try {

                String command = reader.readLine();

                switch (command) {

                    case "stop": ;
                    case "quit": ;
                    case "exit": break loop;
                    default: System.out.println("Unknown command: " + command); break;
                }

            } catch (IOException ex) {

                LOG.throwing(this.getClass().toString(), "start", ex);

            }
        }

        hwtcpServer.stop();

        //hwGroups.forEach((k, v) -> v.flushToFiles());
        //HomeWorks are flushed on addition, this is currently not needed
        //However caching may come back-> This is a reminder

        return this;

    }

    /**
     * @param args This should be the start arguments (of the application)
     */
    public HWServer setArgs(String[] args) {

        ArrayList<AppArgument> arguments = AppArgument.fromArray(args);

        arguments.forEach(this::setArg);

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
     * Sets one specific argument. Remember: Args cannot be removed, just adjusted
     * @param a The argument as AppArgument
     *          @see AppArgument;
     * @return this
     */
    public HWServer setArg(AppArgument a) {

        if (a.getKey().startsWith("--u:")) {

            startArgs.add(new AppArgument("--" + a.getKey().substring(4), a.getValue()));
            return this;

        }


        if (a.getValue() != null && !a.getValue().equals("")) {

            String key = a.getKey();
            String value = a.getValue();

            switch (key) {
                case "--config": confFile = value; break;
                default: LOG.warning("Unsupported argument: " + key); break;
            }

        } else {
            switch (a.getKey()) {
                case "--debug": enableDebug(); break;
                case "--log-no-trace": LOGFORMATTER.setDebug(false); break;
                default: LOG.warning("Unsupported argument: " + a.getKey()); break;
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
        HCONSOLESTD.setLevel(Level.FINEST);

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

    public CommHandProvider getCommandHandlerProvider() { return myCommHandProvider; }

    public AuthProvider getAuthProvider() { return myAuthProvider; }

    public synchronized Optional<HWGroup> getGroup(String group) {

        HWGroup hwGroup = null;

        if (hwGroups.containsKey(group)) {

            hwGroup = hwGroups.get(group);

        }

        return Optional.ofNullable(hwGroup);

    }

    public Optional<SSLServerSocketFactory> getSecureSocketFactory() {
        //I know, Optional is currently optional itself, but this code may change in future updates

        SSLServerSocketFactory ssf = null;

        if (getConfig().getJSON().has("secure_tcp_key") && getConfig().getJSON().has("secure_tcp_password")) {

            System.setProperty("javax.net.ssl.keyStore", getConfig().getJSON().getString("secure_tcp_key"));
            System.setProperty("javax.net.ssl.keyStorePassword", getConfig().getJSON().getString("secure_tcp_password"));

        }

        ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        return Optional.ofNullable(ssf);

    }

}
