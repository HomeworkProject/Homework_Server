package de.mlessmann.logging;

import java.io.PrintStream;
import java.util.logging.*;

/**
 * Created by Life4YourGames on 29.04.16.
 */
public class HWConsoleHandler extends ConsoleHandler{

    /**
     * Create a new console Handler using out as OutputStream
     * @param out
     */
    public HWConsoleHandler(PrintStream out) {

        super();

        //Use default as default
        if (out == null) {
            out = System.out;
        }

        setOutputStream(out);
    }

}
