package de.mlessmann.hwserver;

import de.mlessmann.config.HWConfig;
import de.mlessmann.logging.HWLogFormatter;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import de.mlessmann.logging.*;



/**
 * Created by Life4YourGames on 29.04.16.
 */
public class HWServer {

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
     * FileHandler for writing the "latest"-log
     */
    private FileHandler logFileHandler;

    /**
     * Configuration object (using JSON)
     * @see #getConfig
     */
    private HWConfig config;

    /**
     * Create a new Server instance
     * Currently multiple instances are not natively implemented
     */
    public HWServer() throws IOException {

        logFileHandler = new FileHandler(getLogFile());
        logFileHandler.setFormatter(LOGFORMATTER);

        //Replace the default console formatter
        LOG.addHandler(new HWConsoleHandler(System.out));
        LOG.getHandlers()[0].setFormatter(LOGFORMATTER);
        LOG.addHandler(logFileHandler);

        LOG.fine("Entering initialization");

        //Create, open and read config
        config = new HWConfig(this);
        if (!config.open("config.json").isInitialized()) {
            LOG.severe("Unable to read config! This instance is not going to work!");
            throw new IOException("Config not readable");
        }


    }

    /**
     * @param args This should be the start arguments (of the application)
     */
    public void setArgs(String[] args) {
        if (args.length > 0) {
            Arrays.stream(args).filter(s -> !"-debug".equals(s.toLowerCase())).forEach(s -> LOG.info("Unsupported argument: " + s));
        }
    }

    //Return "main" (HW) logger
    public Logger getLogger() {
        return LOG;
    }

    //Return logFile name
    public String getLogFile() {
        return LOGFILE;
    }

}
