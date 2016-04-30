package de.mlessmann.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Created by Life4YourGames on 29.04.16.
 */
public class HWLogFormatter extends java.util.logging.Formatter {

    public Boolean isDebug = false;

    public String format(LogRecord lrec) {

        if (lrec.getThrown() == null) {
            //No exception -> Standard formatting
            StringBuffer buff = new StringBuffer();

            buff.append(calcDate(lrec.getMillis()));
            buff.append(" ")
                    .append(lrec.getLoggerName());
            buff.append(" ")
                    .append(lrec.getLevel());
            buff.append(" ")
                    .append(lrec.getMessage());
            buff.append("\n");
            if (isDebug) {
                buff.append("--> Sent by:")
                        .append(lrec.getSourceClassName()).append(" :: ")
                        .append(lrec.getSourceMethodName());
            }
            buff.append("\n");

            return buff.toString();

        } else {
            //Fall back to standard error format
            return super.formatMessage(lrec);
        }

    }

    private String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Date rdate = new Date(millisecs);
        return date_format.format(rdate);
    }

}
