package de.mlessmann.updates;

import de.mlessmann.common.Common;
import de.mlessmann.common.HTTP;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.updates.indices.IRelease;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 03.10.16.
 */
public class HWUpgrade {

    private HWServer server;
    private IRelease release;

    public HWUpgrade(HWServer server) {
        this.server = server;
    }

    public void setRelease(IRelease r) { release = r; }

    public boolean downloadFiles() {
        if (release==null) {
            server.onMessage(this, Level.SEVERE, "No release selected");
            return false;
        }

        Map<String, String> files = release.files();
        if (files.isEmpty()) {
            server.onMessage(this, Level.SEVERE, "Release has no files");
            return false;
        }
        boolean[] s = {true};
        files.forEach((p, u) -> {
            if (!s[0]) return;
            try {
                if (p.startsWith("i_update_") && p.endsWith(".zip")) {
                    if (!HTTP.GETFILE(u, new File("cache/update/Homework_Server.zip"))) {
                        server.onMessage(this, Level.SEVERE, "Unable to download update file: returned false");
                        s[0] = false;
                    }
                } else {
                    server.onMessage(this, Level.FINER, "Skipping non-update file: " + p);
                }
            } catch (IOException e) {
                server.onMessage(this, Level.SEVERE, "Unable to download update file: " + e.toString());
                server.onException(this, Level.SEVERE, e);
                s[0] = false;
            }
        });
        return s[0];
    }

    public boolean genConfig() {
        File f = new File("install.json");
        /*
        if (!f.canWrite()) {
            server.onMessage(this, Level.SEVERE, "Upgrade config not writable!");
            return false;
        }
        */

        JSONObject conf = new JSONObject();
        JSONArray actions = new JSONArray();

        actions.put(new JSONObject("        {\n" +
                "            \"name\": \"extractUpdate\",\n" +
                "            \"runtime\": \"RUN\",\n" +
                "            \"type\": \"EXTRACT\",\n" +
                "            \"from\": \"cache/update/HomeWork_Server.zip\",\n" +
                "            \"to\": \".\"\n" +
                "        }"));

        JSONObject runTask = new JSONObject();
        runTask.put("name", "runAfterInstall");
        runTask.put("runtime", "FINALIZE");
        runTask.put("type", "RUN");
        runTask.put("inheritIO", server.getConfig().getNode("update", "inheritIO").optBoolean(true));

        JSONArray command = new JSONArray();
        String java  = System.getProperty("java.home") + "\\bin\\java";
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String[] comm = System.getProperty("sun.java.command").split(" ");

        command.put(java);
        jvmArgs.forEach(command::put);
        command.put("-cp");
        //command.put(System.getProperty("java.class.path"));
        command.put("libraries/*;Homework_Server.jar");
        Arrays.stream(comm).forEach(command::put);
        runTask.put("command", command);
        actions.put(runTask);

        conf.put("actions", actions);

        try (FileWriter w = new FileWriter(f)) {
            w.write(conf.toString(2));
            w.flush();
        } catch (IOException e) {
            server.onMessage(this, Level.SEVERE, "Unable to write upgradeConfig: " + e.toString());
            server.onException(this, Level.SEVERE, e);
            return false;
        }
        return true;
    }

    public boolean installUpgrade() {
        File fInstaller = new File("libraries/installer.jar");
        File newInstaller = new File("installer.jar");
        server.onMessage(this, Level.FINE, "Copying installer");
        try {
            Common.copyFile(fInstaller, newInstaller);
        } catch (IOException e) {
            server.onMessage(this, Level.SEVERE, "Unable to set up installer: " + e.toString());
            server.onException(this, Level.SEVERE, e);
            return false;
        }

        String java = System.getProperty("java.home") + "\\bin\\java";
        List<String> command = new ArrayList<String>();
        command.add(java);
        command.add("-jar");
        command.add("installer.jar");
        command.add("--upgrade");
        command.add("--delay=10000");

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(command);
        pb.inheritIO();
        try {
            pb.start();
            return true;
        } catch (IOException e) {
            server.onMessage(this, Level.SEVERE, "Unable to start upgrade: " + e.toString());
            server.onException(this, Level.SEVERE, e);
            return false;
        }
    }

}
