package de.mlessmann.hwserver;

import de.mlessmann.allocation.GroupMgrSvc;
import de.mlessmann.common.apparguments.AppArgument;
import de.mlessmann.config.ConfigNode;
import de.mlessmann.config.JSONConfigLoader;
import de.mlessmann.config.api.ConfigLoader;
import de.mlessmann.hwserver.services.sessionsvc.SessionMgrSvc;
import de.mlessmann.hwserver.services.updates.IRelease;
import de.mlessmann.hwserver.services.updates.IUpdateSvcListener;
import de.mlessmann.hwserver.services.updates.UpdateSvc;
import de.mlessmann.logging.HWConsoleHandler;
import de.mlessmann.logging.HWLogFormatter;
import de.mlessmann.logging.ILogReceiver;
import de.mlessmann.network.HWTCPServer;
import de.mlessmann.reflections.AuthLoader;
import de.mlessmann.reflections.AuthProvider;
import de.mlessmann.reflections.CommHandProvider;
import de.mlessmann.reflections.CommandLoader;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class HWServer implements ILogReceiver, IUpdateSvcListener {

    public static String VERSION = "0.0.0.4";

    //Collect start arguments to pass them through to the updater
    private ArrayList<AppArgument> startArgs = new ArrayList<AppArgument>();

    /**
     * Updater
     */
    private UpdateSvc updateSvc;

    /**
     * CommandLine Handler
     */
    CommandLine commandLine;

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
    private ConfigLoader confLoader;

    /**
     * Root node of configuration
     */
    private ConfigNode config;

    /**
     * Service to manage groups
     * @see #getGroup(String)
     */
    private GroupMgrSvc groupMgrSvc;

    /**
     * Provider for commandHandlers
     */
    private CommHandProvider myCommHandProvider;

    /**
     * Provider for Authentication Methods
     */
    private AuthProvider myAuthProvider;

    /**
     * Management of Sessions
     */
    private SessionMgrSvc sessionMgrSvc;

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

        //LOG.setLevel(Level.FINEST);

        LOG.setUseParentHandlers(false);

        //Replace the default console formatter
        HCONSOLESTD.setFormatter(LOGFORMATTER);
        HCONSOLEERR.setFormatter(LOGFORMATTER);
        LOG.addHandler(HCONSOLESTD);
        LOG.addHandler(HCONSOLEERR);
        LOG.addHandler(logFileHandler);

        LOG.info("------Entering preInitialization------");

        //PreInit config so the reference is correct
        confLoader = new JSONConfigLoader();

        config = confLoader.loadFromFile(confFile);

        if (confLoader.hasError()) {

            confLoader.getError().printStackTrace();
            LOG.severe("Unable to read JSON-Conf: Falling back to defaults...");
            config = new ConfigNode();
            LOG.severe("Attempting to save an empty root node configuration...");
            confLoader.save(config);
            //This save does not have to be successful
            confLoader.resetError();

        }

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

        //-----------------------------------------------------------------------------------
        //--------------------------- Config Init -------------------------------------------
        ConfigNode node;

        node = config.getNode("groups");
        if (node.isVirtual()) {
            LOG.info("No group node present: Will be initialized");
        }
        //--------------------------- END Config Init ---------------------------------------
        //-----------------------------------------------------------------------------------

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

        groupMgrSvc = new GroupMgrSvc(this);
        if (!groupMgrSvc.init(config.getNode("groups"))) {
            String msg = "Unable to initialize groups!";
            LOG.severe(msg);
            throw new IOException(msg);
        }
        LOG.info(groupMgrSvc.getGroups().size() + " groups loaded");

        // -------------------------------- POST INIT --------------------------------
        // -------------------------------- POST INIT --------------------------------
        // -------------------------------- POST INIT --------------------------------

        LOG.info("------Entering post-initialization------");

        LOG.info("Initializing SessionMgrSvc");
        sessionMgrSvc = new SessionMgrSvc(this);

        LOG.info("Initializing UpdateSvc");
        updateSvc = new UpdateSvc(this);
        updateSvc.registerListener(this);

        LOG.info("Initializing commandLine");
        commandLine = new CommandLine(this);

        return this;
    }

    /**
     * Start the server
     */

    public HWServer start() {
        if (hwtcpServer != null && !hwtcpServer.isStopped())
            return this;

        hwtcpServer= new HWTCPServer(this);

        if (!hwtcpServer.setUp()) {

            LOG.severe("An error occurred while setting up the tcp connections, this instance is going silent now!");
            return this;

        }

        hwtcpServer.start();

        commandLine.run();

        return this;

    }

    public HWServer stop() {

        if (hwtcpServer.isStopped())
            return this;
        hwtcpServer.stop();

        //hwGroups.forEach((k, v) -> v.flushToFiles());
        //HomeWorks are flushed on addition, this is currently not needed
        //However caching may come back-> This is a reminder

        confLoader.save(config);
        if (confLoader.hasError()) {
            confLoader.getError().printStackTrace();
            LOG.severe("Unable to save configuration!");
        }

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
                case "--mimic-version": VERSION = value; break;
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
    public ConfigNode getConfig() {
        return config;
    }

    public CommHandProvider getCommandHandlerProvider() { return myCommHandProvider; }

    public AuthProvider getAuthProvider() { return myAuthProvider; }

    public SessionMgrSvc getSessionMgr() { return sessionMgrSvc; }

    public GroupMgrSvc getGroupManager() {
        return groupMgrSvc;
    }

    public Optional<SSLServerSocketFactory> getSecureSocketFactory() {
        //I know, Optional is currently optional itself, but this code may change in future updates

        SSLServerSocketFactory ssf = null;

        if (!getConfig().getNode("secure_tcp_key").isVirtual() && !getConfig().getNode("secure_tcp_password").isVirtual()) {

            System.setProperty("javax.net.ssl.keyStore", getConfig().getNode("secure_tcp_key").optString(null));
            System.setProperty("javax.net.ssl.keyStorePassword", getConfig().getNode("secure_tcp_password").optString(null));

        }

        ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        return Optional.ofNullable(ssf);

    }

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- --- --- --- --- --- --- --- --- --- ---  Interfaces --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    //ILogReceiver
    @Override
    public void onMessage(Object sender, Level level, String message) {
        LOG.log(level, message);
    }

    @Override
    public void onException(Object sender, Level level, Exception e) {
        e.printStackTrace();
    }

    //UpdateSvcListener
    public void checkForUpdate() {
        if (updateSvc.prepare()) {
            LOG.info("Checking for updates...");
        } else {
            LOG.warning("Unable to check for updates: Wait for svc to finish");
        }
    }

    public void upgrade() {
        if (updateSvc.upgrade()) {
            LOG.info("Starting upgrade...");
        } else {
            LOG.warning("Unable to start upgrade: Wait for svc to finish");
        }
    }


    @Override
    public void onSvcStart() {
        LOG.info("UpdateSvc started: Update commands locked");
    }


    @Override
    public void onSvcDone(boolean failed) {
        LOG.info("UpdateSvc reported exit("+(failed?"FAIL":"SUCCESS")+"): Update commands now available again");
    }

    @Override
    public void onUpdateAvailable(IRelease r) {
        LOG.severe("AN UPDATE IS AVAILABLE: " + r.getVersion());
    }

    @Override
    public void onUpdateDownloaded() {
        LOG.severe("An update has been downloaded.");
    }

    @Override
    public void onUpgradeAboutToStart(boolean immediate) {
        if (immediate) {
            commandLine.exit(true);
        }
    }

    @Override
    public void onUpgradeFailed() {
        LOG.severe("An upgrade FAILED! You may need to resolve this!");
    }
}
