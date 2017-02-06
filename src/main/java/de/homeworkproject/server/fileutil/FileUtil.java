package de.homeworkproject.server.fileutil;

import java.io.*;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 22.06.16.
 */
public class FileUtil {

    private File myFile;
    private Logger LOG = Logger.getGlobal();

    public FileUtil(File fil) {

        myFile = fil;

    }

    public FileUtil(String path) {

        myFile = new File(path);

    }

    public File getFile() { return myFile; }

    public void setLogger(Logger l) { LOG = l; }

    public StringBuilder getContent(boolean withNewLine) {

        BufferedReader buff;
        StringBuilder content;

        try (FileReader fReader = new FileReader(myFile)) {
            //Read the file

            buff = new BufferedReader(fReader);

            content = new StringBuilder();

            buff.lines().forEach(s ->
                {
                    content.append(s);
                    if (withNewLine) {
                        content.append("\n");
                    }
                });

        } catch (FileNotFoundException ex) {

            LOG.warning("Unable to open file: " + myFile + ": " + ex.toString());
            return null;

        } catch (IOException ex) {

            LOG.warning("Unable to read file: " + myFile + ": " + ex.toString());
            return null;

        }

        return content;

    }

    public boolean createIfNonexistent() {

        try {

            return !myFile.exists() && myFile.createNewFile();

        } catch (IOException ex) {

            return false;

        }

    }

    public boolean writeToFile(String content) {

        try (FileWriter writer = new FileWriter(myFile)) {

            writer.write(content);

        } catch (IOException ex) {

            LOG.finer("FUtil#WriteToFile: Unable to write to file: " + myFile.getPath());

            return false;

        }

        return true;

    }

}
