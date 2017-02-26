package de.homeworkproject.server.hwserver;

import de.homeworkproject.server.allocation.GroupMgrSvc;
import de.homeworkproject.server.hwserver.services.sessionsvc.SessionMgrSvc;
import de.homeworkproject.server.logging.HWConsoleHandler;
import de.homeworkproject.server.logging.HWLogFormatter;
import de.homeworkproject.server.network.HWTCPServer;
import de.homeworkproject.server.reflections.AuthLoader;
import de.homeworkproject.server.reflections.AuthProvider;
import de.homeworkproject.server.reflections.CommHandProvider;
import de.homeworkproject.server.reflections.CommandLoader;
import de.homeworkproject.server.tasks.FSCleanTask;
import de.homeworkproject.server.tasks.ITask;
import de.homeworkproject.server.tasks.TaskManager;
import de.homeworkproject.server.updates.HWUpdateManager;
import de.mlessmann.common.apparguments.AppArgument;
import de.mlessmann.common.parallel.IFuture;
import de.mlessmann.common.parallel.IFutureListener;
import de.mlessmann.config.ConfigNode;
import de.mlessmann.config.JSONConfigLoader;
import de.mlessmann.config.api.ConfigLoader;
import de.mlessmann.logging.ILogReceiver;
import de.mlessmann.updates.indices.IRelease;
import de.mlessmann.updates.indices.IndexTypeProvider;
import hwserver.Main;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

//import de.mlessmann.hwserver.services.updates.IRelease;
/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class HWServer implements ILogReceiver, IFutureListener {

    public static final URLClassLoader CLASSLOADER = (URLClassLoader) Thread.currentThread().getContextClassLoader();

    public static String VERSION = "0.0.0.10";

    //Collect start arguments to pass them through to the updater
    private ArrayList<AppArgument> startArgs = new ArrayList<AppArgument>();

    /**
     * Updater
     */
    private HWUpdateManager updateMgr;
    private boolean updateWasScheduled = false;

    /**
     * Scheduled Tasks
     */
    private TaskManager taskMgr;

    /**
     * CommandLine Handler
     */
    private CommandLine commandLine;

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
     * Should we skip the configurator and fall back to defaults when the config is missing?
     */
    private boolean skipToDefault = false;

    /**
     * Should we start the FirstTimeConfigurator on startup?
     * I do not recommend this...
     */
    private boolean forceFirstTimeConfigurator;

    /**
     * Path to the config
     * @see #setArg(AppArgument)
     */
    private String confFile = "conf/config.json";
    private String groupConfig = null;

    /**
     * Configuration object (using JSON)
     * @see #getConfig
     */
    private ConfigLoader confLoader;
    private ConfigLoader gConfigLoader = null;

    /**
     * Root node of configuration
     */
    private ConfigNode config;
    private ConfigNode gConfig = null;

    /**
     * Service to manage groups
     * @see #getGroupManager()
     * @see GroupMgrSvc#getGroup(String)
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

    private ScheduledFuture<?> updateSchedule;
    private IRelease update;
    private ScheduledThreadPoolExecutor scheduledUpdateExecutor;

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

        onMessage(this, FINE, "Preparing configuration");
        //PreInit config so the reference is correct
        confLoader = new JSONConfigLoader();
        config = confLoader.loadFromFile(confFile);

        if (confLoader.hasError()) {
            File f = new File(confFile);
            if (f.isFile()) {
                throw new RuntimeException("Unable to read JSON-Conf: Falling back to defaults...", confLoader.getError());
            } else {
                confLoader.resetError();
                config = new ConfigNode();
                if (skipToDefault) {
                    onMessage(this, WARNING, "Configuration not found: Falling back to empty defaults.");
                } else {
                    onMessage(this, WARNING, "Configuration not found: Running setup");
                    Main.setupConfiguration(config);
                }
            }
            onMessage(this, INFO, "Attempting to save configuration...");
            confLoader.save(config);
            //This save does not have to be successful
            confLoader.resetError();
        }
        if (forceFirstTimeConfigurator) {
            Main.setupConfiguration(config);
            confLoader.save(config);
            //This save does not have to be successful
            confLoader.resetError();
        }

        onMessage(this, INFO, "Initializing TaskManager");
        taskMgr = new TaskManager(this);

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
        onMessage(this, INFO, "------Entering initialization------");

        onMessage(this, FINER, "Setting ClassLoaders");
        CommandLoader.loader = CLASSLOADER;
        AuthLoader.loader = CLASSLOADER;
        IndexTypeProvider.loader = CLASSLOADER;
        onMessage(this, FINEST, "Classpath entries:");
        onMessage(this, FINEST, System.getProperty("java.class.path"));

        // --- Command Handler ---

        onMessage(this, FINE, "Attempting to load CommandHandler");

        myCommHandProvider = new CommHandProvider();
        myCommHandProvider.setLogger(LOG);

        CommandLoader cLoader = new CommandLoader();

        cLoader.setProvider(myCommHandProvider);
        cLoader.setLogger(LOG);
        cLoader.setMaster(this);

        cLoader.loadAll();

        onMessage(this, FINE, myCommHandProvider.getHandler().size() + " handler registered.");

        // --- Auth Methods ---

        onMessage(this, FINE, "Attempting to load AuthMethods");

        myAuthProvider = new AuthProvider();
        myAuthProvider.setLogger(LOG);

        AuthLoader aLoader = new AuthLoader();

        aLoader.setProvider(myAuthProvider);
        aLoader.setLogger(LOG);
        aLoader.setMaster(this);

        aLoader.loadAll();

        onMessage(this, FINE, myAuthProvider.getMethods().size() + " methods registered.");

        // --- Config ---

        onMessage(this, FINE, "Trying to load configuration");

        File confDir = new File("conf");

        if (!confDir.isDirectory()) {
            if (!confDir.mkdir()) {
                onMessage(this, SEVERE, "Unable to create dir \"conf\"!");
                throw new IOException("Cannot create directory: "+ confDir.getAbsolutePath());
            } else {
                onMessage(this, INFO, "Created default configuration directory");
            }
        }

        //-----------------------------------------------------------------------------------
        //--------------------------- Config Init -------------------------------------------
        ConfigNode node;

        //Groups
        node = config.getNode("groups");
        if (node.isVirtual()) {
            onMessage(this, INFO, "No group node present: Will be initialized");
        }
        //Update
        node = config.getNode("update", "enable");
        if (node.isVirtual()) node.setBoolean(true);
        node = config.getNode("update", "interval");
        if (node.isVirtual()) node.setInt(1);
        node = config.getNode("update", "intervalTimeUnit");
        if (node.isVirtual()) node.setString("HOURS");
        //Cleanup
        node = config.getNode("cleanup", "hw_database", "enable");
        if (node.isVirtual()) node.setBoolean(false);
        node = config.getNode("cleanup", "hw_database", "maxAgeDays");
        if (node.isVirtual()) node.setInt(60);
        node = config.getNode("cleanup", "hw_database", "interval");
        if (node.isVirtual()) node.setInt(1);
        confLoader.save(config);
        //--------------------------- END Config Init ---------------------------------------
        //-----------------------------------------------------------------------------------

        File groupDir = new File("groups");

        if (groupDir.isFile()) {
            String msg = "File \"groups\" is occupying the groups directory! Delete or move the file before starting the server";
            onMessage(this, SEVERE, msg);
            throw new FileAlreadyExistsException(msg);
        }

        if (!groupDir.isDirectory() && !groupDir.mkdirs()) {
            String msg = "Unable to create group directory!";
            onMessage(this, SEVERE, msg);
            throw new IOException(msg);
        }

        groupMgrSvc = new GroupMgrSvc(this);
        if (config.getNode("groups").isHub()) {
            gConfig = config.getNode("groups");
        } else if (config.getNode("groups").isType(String.class)) {
            groupConfig = config.getNode("groups").optString("groups.json");
            onMessage(this, FINE, "Loading groups from " + gConfig);
            gConfigLoader = new JSONConfigLoader();
            gConfig = gConfigLoader.loadFromFile(groupConfig).getNode("groups");
            if (gConfigLoader.hasError()) {
                onMessage(this, SEVERE, "Unable to load groups: " + gConfigLoader.getError().getClass().getSimpleName());
                onException(this, SEVERE, gConfigLoader.getError());
                throw new IOException("Unable to load groups from external file", gConfigLoader.getError());
            }
        }
        if (!groupMgrSvc.init(gConfig)) {
            String msg = "Unable to initialize GroupMgrSvc!";
            onMessage(this, SEVERE, msg);
            throw new IOException(msg);
        }

        onMessage(this, INFO, groupMgrSvc.getGroups().size() + " groups loaded");

        // -------------------------------- POST INIT --------------------------------
        // -------------------------------- POST INIT --------------------------------
        // -------------------------------- POST INIT --------------------------------

        onMessage(this, INFO, "------Entering post-initialization------");

        onMessage(this, INFO, "Initializing SessionMgrSvc");
        sessionMgrSvc = new SessionMgrSvc(this);

        onMessage(this, INFO, "Initializing UpdateSvc and scheduling update if enabled");
        updateMgr = new HWUpdateManager(this);
        scheduleUpdate();

        onMessage(this, INFO, "Starting install-cleanup task");
        new FSCleanTask(this).setFiles(FSCleanTask.getInstallRemnants()).run();


        onMessage(this, INFO, "Initializing commandLine");
        commandLine = new CommandLine(this);
        commandLine.setIn(System.in);
        commandLine.setOut(System.out);

        return this;
    }

    /**
     * Start the server
     */

    public HWServer start() {
        if (hwtcpServer != null && !hwtcpServer.isStopped())
            return this;
        hwtcpServer = new HWTCPServer(this);
        if (!hwtcpServer.setUp()) {
            onMessage(this, SEVERE, "An error occurred while setting up the tcp connections, this instance is going silent now!");
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

        taskMgr.shutdown(false);

        //hwGroups.forEach((k, v) -> v.flushToFiles());
        //HomeWorks are flushed on addition, this is currently not needed
        //However caching may come back-> This is a reminder
        confLoader.save(config);
        if (confLoader.hasError()) {
            confLoader.getError().printStackTrace();
            onMessage(this, SEVERE, "Unable to save configuration!");
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
                case "--debug": enableDebug(a.getValue()); break;
                default: LOG.warning("Unsupported argument: " + key); break;
            }
        } else {
            switch (a.getKey()) {
                case "--debug": enableDebug(null); break;
                case "--log-no-trace": LOGFORMATTER.setDebug(false); break;
                //---HIDDEN DO NOT PUBLISH IN DOCS--
                case "--force-configurator": forceFirstTimeConfigurator = true; break;
                //---END HIDDEN DO NOT PUBLISH IN DOCS--
                default: LOG.warning("Unsupported argument: " + a.getKey()); break;
            }
        }
        return this;
    }

    /**
     * Enable debug mode for this instance
     * A level can be specified to limit the verbosity of the output
     * @return this
     */
    public HWServer enableDebug(String val) {
        if (val == null || val.isEmpty()) {
            setDebugLevel(Level.FINEST);
            onMessage(this, INFO, "Debug mode enabled");
        } else {
            switch (val) {
                case "DEBUG":
                case "FINEST":
                    setDebugLevel(Level.FINEST);
                    break;
                case "FINER":
                    setDebugLevel(Level.FINER);
                    break;
                case "FINE":
                    setDebugLevel(Level.FINE);
                    break;
                case "INFO":
                    setDebugLevel(Level.INFO);
                    break;
                case "WARN":
                    setDebugLevel(Level.WARNING);
                    break;
                case "ERROR":
                case "ERR":
                    setDebugLevel(Level.SEVERE);
                    break;
                default:
                    onMessage(this, INFO, "Unknown debug level: Falling to FINEST");
                    setDebugLevel(FINEST);
                    break;
            }
        }
        return this;

    }

    private void setDebugLevel(Level level) {
        LOG.setLevel(level);
        if (level.intValue() >= Level.FINE.intValue()) {
            LOGFORMATTER.setDebug(true);
        } else {
            LOGFORMATTER.setDebug(false);
        }
        HCONSOLESTD.setLevel(level);
        onMessage(this, INFO, "Debug mode enabled");
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

    public Optional<ServerSocketFactory> getSecureSocketFactory() {
        //I know, Optional is currently optional itself, but this code may change in future updates
        ServerSocketFactory ssf = null;
        if (!getConfig().getNode("tcp", "ssl", "key").isVirtual() && !getConfig().getNode("tcp", "ssl", "password").isVirtual()) {
            System.setProperty("javax.net.ssl.keyStore", getConfig().getNode("tcp", "ssl", "key").optString(null));
            System.setProperty("javax.net.ssl.keyStorePassword", getConfig().getNode("tcp", "ssl", "password").optString(null));
        }

        ssf = SSLServerSocketFactory.getDefault();
        return Optional.ofNullable(ssf);

    }

    public TaskManager getTaskMgr() { return taskMgr; }

    public HWTCPServer getTCPServer() { return hwtcpServer; }

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- --- --- --- --- --- --- --- --- --- ---  Interfaces --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    //ILogReceiver
    @Override
    public void onMessage(Object sender, Level level, String message) {
        if (sender != null) {
            String name = sender.getClass().getSimpleName();
            LOG.log(level, name + ' ' + message);
        } else {
            LOG.log(level, message);
        }
    }

    @Override
    public void onException(Object sender, Level level, Exception e) {
        e.printStackTrace();
    }

    // --- --- --- --- --- --- --- --- --- --- Updates --- --- --- --- --- --- --- --- --- --- --- --- ---

    public synchronized void checkForUpdate() {
        startUpdateCheck();
    }

    private void startUpdateCheck() {
        IFuture<IRelease> f = updateMgr.checkForUpdate();
        if (f!=null) {
            onMessage(this, INFO, "Checking for update...");
            f.registerListener(this);
        } else
            onMessage(this, WARNING, "Unable to check for update: Manager busy...");
    }

    public synchronized void upgrade() {
        IFuture<IRelease> f = updateMgr.getUpdateFuture();
        if (f==null) {
            onMessage(this, WARNING, "Check for update before upgrading!");
            return;
        }
        IRelease r = f.getOrElse(null);
        if (r == null) {
            onMessage(this, WARNING, "No upgrade available!");
            return;
        }
        startUpgrade(r);
    }

    private void startUpgrade(IRelease release) {
        IFuture<Boolean> f = updateMgr.upgrade(release);
        if (f!=null) {
            onMessage(this, INFO, "Starting upgrade...");
            f.registerListener(this);
        } else
            onMessage(this, WARNING, "Unable to start upgrade: Manager busy...");
    }

    public synchronized void scheduleUpdate() {
        if (!config.getNode("update", "enable").optBoolean(true)) return;

        int del = config.getNode("update", "interval").optInt(60*60);
        TimeUnit tU = TimeUnit.valueOf(config.getNode("update", "intervalTimeUnit").optString("SECONDS"));
        if (del == 0)
            return;
        taskMgr.schedule(new ITask() {
            @Override
            public int getInterval() {
                return del;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return tU;
            }

            @Override
            public void reportTaskManager(TaskManager mgr) {

            }

            @Override
            public void run() {
                updateWasScheduled = true;
                IFuture<IRelease> uFuture = updateMgr.getUpdateFuture();
                if (uFuture == null) {
                    onMessage(this, FINE, "Never checked for updates before: check will be performed");
                    startUpdateCheck();
                    return;
                } else {
                    IRelease release = uFuture.getOrElse(null);
                    if (release == null) {
                        onMessage(this, FINE, "No update previously found: check will be performed");
                        startUpdateCheck();
                        return;
                    } else {

                    }
                }
                scheduleUpdate();
            }
        });

        onMessage(this, FINE, "Update scheduled in " + del + ' ' + tU.toString());
    }

    @Override
    public void onFutureAvailable(IFuture<?> future) {
        if (future == updateMgr.getUpdateFuture()) {
            IRelease r = (IRelease)future.getOrElse(null);
            onMessage(this, INFO, "Update check returned: " +
                    (r!=null ? r.version() + " is available!" : "No update available."));

            if (r!=null && updateWasScheduled) {
                boolean autoUpdate = config.getNode("update", "autoUpgrade").optBoolean(true);
                if (!autoUpdate) return;
                onMessage(this, INFO, "An update has been found! Starting upgrade!");
                startUpgrade(r);
                updateWasScheduled = false;
            }
            return;
        }
        if (future == updateMgr.getUpgradeFuture()) {
            //nope
            @SuppressWarnings("unchecked")
            Boolean readyToStart = ((IFuture<Boolean>)future).getOrElse(Boolean.FALSE);
            if (!readyToStart) {
                onMessage(this, SEVERE, "Upgrade setup returned: An error occurred!");
                return;
            } else {
                onMessage(this, SEVERE, "Upgrade setup returned: Ready to upgrade! Starting...");
                if (!updateMgr.getLastUpgrade().installUpgrade()) {
                    onMessage(this, SEVERE, "Unable to start upgrade...!");
                    return;
                }
                onMessage(this, SEVERE, "Upgrade imminent: Shutting down!");
                commandLine.exit(true);
                return;
            }
        }
    }
}
