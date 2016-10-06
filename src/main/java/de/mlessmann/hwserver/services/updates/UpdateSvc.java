package de.mlessmann.hwserver.services.updates;

import de.mlessmann.common.HTTP;
import de.mlessmann.hwserver.HWServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.logging.Level.*;

/**
 * Created by Life4YourGames on 17.09.16.
 */
public class UpdateSvc implements Runnable {

    private HWServer server;
    private UpdateSvcMode mode;
    private volatile Thread myThread;

    private List<IUpdateSvcListener> listeners = new ArrayList<IUpdateSvcListener>();

    private boolean drafts = false;
    private boolean preReleases = true;//TODO: CHANGE DEFAULT ON RELEASE
    private String branch = null;
    private List<IRelease> releases = new ArrayList<IRelease>();
    private IRelease latest;

    private boolean scheduleDownload;

    public UpdateSvc(HWServer server) {
        this.server = server;
    }

    /**
     * @deprecated Only for internal use!
     * @see #update()
     * @see #download()
     * @see #upgrade()
     */
    @Deprecated
    public void run() {
        sendStateStart();
        if (myThread==null) {
            myThread = new Thread(this);
            myThread.start();
            return;
        }
        boolean failed = false;
        scheduleDownload = false;

        switch (mode) {
            case UPDATE: failed = !doUpdate(); break;
            case DOWNLOAD: scheduleDownload = true; break;
            case UPGRADE: failed = !doUpgrade(); break;
            case PREPARE: failed = !doUpdate(); break;
            default: failed=true; break;
        }

        if (scheduleDownload)
            failed = failed || !doDownload();

        myThread = null;
        sendStateDone(!failed);
        if (!failed && scheduleDownload)
            sendStateDownloaded();
        if (failed && mode == UpdateSvcMode.UPGRADE)
            sendStateUpgradeFailed();
        if (!failed && (mode == UpdateSvcMode.UPDATE || mode == UpdateSvcMode.PREPARE) && latest == null)
            sendNoUpdateAvailable();
    }

    public boolean update() {
        if (isBusy())
            return false;
        mode = UpdateSvcMode.UPDATE;
        run();
        return true;
    }

    public boolean download() {
        if (isBusy())
            return false;
        mode = UpdateSvcMode.DOWNLOAD;
        run();
        return true;
    }

    public boolean prepare() {
        if (isBusy())
            return false;
        mode = UpdateSvcMode.PREPARE;
        run();
        return true;
    }

    public boolean upgrade() {
        if (isBusy())
            return false;
        mode = UpdateSvcMode.UPGRADE;
        run();
        return true;
    }

    public boolean isBusy() {
        return !((myThread==null)||(myThread.isAlive()));
    }

    public void registerListener(IUpdateSvcListener l) {
        listeners.add(l);
    }

    public void unregisterListener(IUpdateSvcListener l) {
        listeners.remove(l);
    }

    // --- --- --- --- --- --- --- --- --- --- Internals --- --- --- --- --- --- --- --- --- ---

    private void sendStateStart() {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onSvcStart();
    }

    private void sendStateDone(boolean success) {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onSvcDone(success);
    }

    private void sendUpdateAvailable(IRelease r) {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onUpdateAvailable(r);
    }
    private void sendNoUpdateAvailable() {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onNoUpdateAvailable();
    }

    private void sendStateDownloaded() {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onUpdateDownloaded();
    }

    private void sendStateUpgradeFailed() {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onUpgradeFailed();
    }

    private void sendUpgradeAboutToStart(boolean immediate) {
        for (int i = listeners.size() - 1; i>=0; i--)
            listeners.get(i).onUpgradeAboutToStart(immediate);
    }

    private boolean doUpdate() {
        releases.clear();
        latest = null;
        String index;
        try {
            index = HTTP.GET("https://api.github.com/repos/MarkL4YG/Homework_Server/releases", null);
        } catch (IOException e) {
            server.onMessage(this, Level.WARNING, "Unable to get update index: Exception");
            server.onException(this, Level.WARNING , e);
            return false;
        }
        try {
            JSONArray a = new JSONArray(index);
            a.forEach(o -> {
                if (o instanceof JSONObject) {
                    tryAddRelease(((JSONObject) o));
                }
            });
        } catch (JSONException e) {
            server.onMessage(this, Level.WARNING, "Unable to read update index: JSONException");
            server.onException(this, Level.WARNING, e);
            return false;
        }
        final String[] v = {HWServer.VERSION};
        releases.forEach(r -> {
            if (!drafts && r.isDraft())
                return;
            if (!preReleases && r.isPreRelease())
                return;
            if (branch!=null && !branch.equals(r.getBranch()))
                return;

            if (r.compareTo(v[0]) == -1) {
                latest = r;
                v[0] = latest.getVersion();
            }
        });
        if (latest!=null) {
            sendUpdateAvailable(latest);
            if (mode == UpdateSvcMode.PREPARE) {
                scheduleDownload = true;
            }
        }
        return true;
    }

    private boolean tryAddRelease(JSONObject j) {
        boolean valid =
                j.has("url")
                        && j.has("id")
                        && j.has("tag_name")
                        && j.has("target_commitish")
                        && j.has("draft")
                        && j.has("prerelease")
                        && j.has("assets")
                        //just a divider
                        && (j.get("url") instanceof String)
                        && (j.get("id")instanceof Integer)
                        && (j.get("tag_name") instanceof String)
                        && (j.get("target_commitish") instanceof String)
                        && (j.get("draft") instanceof Boolean)
                        && (j.get("prerelease") instanceof Boolean)
                        && (j.get("assets") instanceof JSONArray);
        if (!valid) {
            server.onMessage(this, FINER, "Skipping invalid release");
            return true;
        }
        releases.add(new GitHubRelease(j));
        return true;
    }

    private boolean doDownload() {
        boolean[] s = new boolean[]{true};
        if (latest==null) {
            server.onMessage(this, Level.WARNING, "Unable to download update: No release selected");
            return false;
        }
        Map<String, String> files = latest.getFiles();
        server.onMessage(this, FINE, "Number of files to check for download: " + files.size());
        files.forEach((name, url) -> {
            try {
                if (name.startsWith("i_update") && name.endsWith(".zip")) {
                    File file = new File("cache/update/update.zip");
                    server.onMessage(this, INFO, "Downloading \""+url+"\" to \""+file.getAbsolutePath()+"\"");
                    s[0] = s[0] && HTTP.GETFILE(url, file);
                } else {
                    server.onMessage(this, FINER, "Skipping \""+name+"\" from download");
                }
            } catch (IOException e) {
                server.onMessage(this, WARNING, "Unable to download file: Exception");
                server.onException(this, WARNING, e);
                s[0] = false;
            }
        });
        return s[0];
    }

    private boolean doUpgrade() {
        sendUpgradeAboutToStart(false);
        server.onMessage(this, FINE, "Copying installer");
        File updater = new File("libraries/installer.jar");
        File installer = new File("installer.jar");
        try {
            Files.copy(updater.toPath(), installer.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            server.onMessage(this, WARNING, "Unable to continue upgrade: Cannot copy installer");
            server.onException(this, WARNING, e);
            return false;
        }

        String javaExecutable = System.getProperty("java.home") + "/bin/java";
        List<String> vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String[] mainCommand = System.getProperty("sun.java.command").split(" ");

        List<String> after_install_exec = new ArrayList<String>();
        after_install_exec.add(javaExecutable);
        vmArgs.forEach(after_install_exec::add);
        after_install_exec.add("-jar");
        Arrays.asList(mainCommand).forEach(after_install_exec::add);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("update_after_install")))) {
            after_install_exec.forEach(s -> {
                try {
                    writer.write(s);
                    writer.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            server.onMessage(this, WARNING, "Unable to continue upgrade: Cannot write auto start file!");
            server.onException(this, WARNING, e);
            return false;
        }

        List<String> insC = new ArrayList<String>();
        insC.add(javaExecutable);
        insC.add("-jar");
        insC.add("installer.jar");
        insC.add("--delay=10000");
        insC.add("--update");

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(insC);
        pb.inheritIO();
        try {
            pb.start();
            sendUpgradeAboutToStart(true);
        } catch (IOException e) {
            server.onMessage(this, WARNING, "Unable to continue upgrade: Cannot start installer!");
            server.onException(this, WARNING, e);
            return false;
        }
        return true;
    }

}
