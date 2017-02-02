package de.mlessmann.hwserver.tasks;

import de.mlessmann.common.FileUtils;
import de.mlessmann.logging.ILogReceiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.logging.Level.*;

/**
 * Created by Life4YourGames on 08.10.16.
 */
public class FSCleanTask {

    public static List<String> getInstallRemnants() {
        List<String> l = new ArrayList<String>();
        l.add("installer.jar");
        l.add("install.json");
        l.add("updateConfig.json");
        l.add("updateIndex.json");
        l.add("cache/update");
        return l;
    }

    private ILogReceiver r;

    private List<String> files;

    public FSCleanTask(ILogReceiver r) {
        this.r = r;
        files = new ArrayList<String>();
    }

    public FSCleanTask setFiles(List<String> files) {
        this.files = files;
        return this;
    }

    public FSCleanTask addFile(String file) {
        files.add(file);
        return this;
    }

    public Thread run() {
        Thread t = new Thread(() -> {
            r.onMessage(this, FINE, "Number of check entries: " + files.size());
            int[] count = {0};
            files.forEach(f -> {
                r.onMessage(this, FINEST, "Checking entry: " + f);
                String[] p = f.split("\\"+File.separatorChar+"");
                File file = null;
                if ("*".equals(p[p.length-1])) {
                    r.onMessage(this, FINER, "wildcard encountered. Tip: Use directories instead.");
                    file = new File(File.separator+"*", "");
                } else {
                    file = new File(f);
                }
                if (!file.exists()) {
                    r.onMessage(this, FINE, "File not found, continuing");
                    return;
                }
                if (!FileUtils.deleteRecursive(file)) {
                    r.onMessage(this, WARNING, "Unable to delete: " + file.getAbsolutePath());
                } else {
                    count[0]++;
                }
            });
            r.onMessage(this, FINE, count[0] + " files deleted");
        });

        t.start();
        return t;
    }
}
