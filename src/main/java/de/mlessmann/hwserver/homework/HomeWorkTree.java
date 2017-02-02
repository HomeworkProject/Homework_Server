package de.mlessmann.hwserver.homework;

import de.mlessmann.common.L4YGRandom;
import de.mlessmann.hwserver.main.HWServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 12.05.16.
 * @author Life4YourGames
 */
public class HomeWorkTree {

    private HWServer server;
    private HomeWorkTree master;
    private Logger log;
    private String baseDirectory;
    private File file;
    // No, children is to long for me >:( !
    private HashMap<String, HomeWorkTree> childs = new HashMap<String, HomeWorkTree>();

    public HomeWorkTree (HWServer hwServer, String baseDir) {
        server = hwServer;
        master = null;
        log = server.getLogger();
        baseDirectory = baseDir;
    }

    public HomeWorkTree (HWServer hwServer, String baseDir, HomeWorkTree myMaster) {
        this(hwServer, baseDir);
        master = myMaster;
    }

    public HomeWorkTree (HWServer hwServer, File baseFile, HomeWorkTree myMaster) {
        this(hwServer, baseFile.getAbsolutePath(), myMaster);
        file = baseFile;
    }

    public File getFile() { return file; }

    public HomeWorkTree analyze() {
        if (file == null) {
            file = new File(baseDirectory);
        }

        if (file.isDirectory()) {

            File[] files = file.listFiles();

            if (files == null) {
                return this;
            }
            //Remove non-directory and non-readable File references
            Arrays.stream(files).filter(File::isDirectory).filter(File::canRead).forEach(this::includeIfDir);

        } else {
            if (!file.mkdir()) {
                log.warning("Unable to index folder \"" + baseDirectory +"\": Cannot perform mkDir");
            } else {
                this.analyze();
            }
        }
        return this;
    }

    public boolean includeIfDir(File subFile) {
        if (subFile.isDirectory()) {
            HomeWorkTree newTree = new HomeWorkTree(server, subFile, this).analyze();
            childs.put(subFile.getName(), newTree);
            return true;
        } else {
            return false;
        }
    }

    public HomeWorkTree getTopMaster() {
        return (master == null) ? this : master.getTopMaster();
    }

    public HomeWorkTree getMaster() {
        return master;
    }

    @Deprecated
    public String makeRelative(String path) {
        if (path.contains(File.separator)) {
            String newFirstPath = path.substring(0, path.indexOf(File.separator) + 1);
            String newSecondPath = path.substring(path.indexOf(File.separator) + 1);
            if (childs.containsKey(newFirstPath)) {
                return path;
            } else {
                return makeRelative(newSecondPath);
            }
        } else {
            if (childs.containsKey(path)) {
                return path;
            } else {
                return null;
            }
        }
    }

    public Optional<HomeWorkTree> getChild(String path) {
        log.finest("HWT#GetChild$" + file.getPath() + ": " + path);
        HomeWorkTree res = null;
            if (path.contains(File.separator)) {
                String newFirstPath;
                String newSecondPath;
                newFirstPath = path.substring(0, path.indexOf(File.separator));
                newSecondPath = path.substring(path.indexOf(File.separator) + 1);
                HomeWorkTree tree = getChild(newFirstPath).orElse(null);
                if (tree != null) {
                    res = tree.getChild(newSecondPath).orElse(null);
                }
            } else {
                if (childs.containsKey(path)) {
                    res = childs.get(path);
                }
            }
        return Optional.ofNullable(res);
    }

    public Optional<HomeWorkTree> getOrCreateChild(String pathToFolder) {
        log.finest("HWT#GetCreateChild$" + file.getPath() + ": " + pathToFolder);
        Optional<HomeWorkTree> child = getChild(pathToFolder);
        if (child.isPresent()) {
            return child;
        }
        child = createChild(pathToFolder);
        return child;
    }

    public Optional<HomeWorkTree> createChild(String pathToFolder) {
        log.finest("HWT#CreateChild$" + file.getPath() + ": " + pathToFolder);
        if (pathToFolder.contains(File.separator)) {
            String first = pathToFolder.substring(0, pathToFolder.indexOf(File.separator));
            String second = pathToFolder.substring(pathToFolder.indexOf(File.separator) + 1);
            Optional<HomeWorkTree> tree = getChild(first);
            if (tree.isPresent()) {
                return tree.get().createChild(second);
            } else {
                Optional<HomeWorkTree> newTree = this.createChild(first);
                if (!newTree.isPresent()){
                    return Optional.empty();
                }
                return newTree.get().createChild(second);
            }
        } else {
            Optional<HomeWorkTree> tree = getChild(pathToFolder);
            if (tree.isPresent()) {
                return tree;
            }
            File fil = new File(file ,pathToFolder);
            log.finest("HWT#CreateChild$" + file.getPath() + "{MKDIR}: " + fil.getPath());
            if (!fil.mkdir()) {
                log.fine("HWT#CreateChild$" + file.getPath() + "{mkdirFAIL}: " + fil.getAbsolutePath());
                return Optional.empty();
            }
            HomeWorkTree newTree = new HomeWorkTree(server, fil, this).analyze();
            childs.put(pathToFolder, newTree);
            return Optional.of(newTree);
        }
    }



    public List<HomeWorkTree> getChilds(){
        ArrayList<HomeWorkTree> res = new ArrayList<HomeWorkTree>();
        childs.forEach((k, v) -> res.add(v));
        return res;
    }

    public List<String> getChildNames(){
        ArrayList<String> res = new ArrayList<String>();
        childs.forEach((k, v) -> res.add(k));
        return res;
    }

    public boolean flushOrCreateFile(String path, String content) {
        boolean res = false;
        File fil = new File (file, path);
        try {
            if (!fil.isFile()) {
                if (!fil.createNewFile()) {
                    log.warning("Unable to create file \"" + fil.getAbsolutePath() + "\": EXISTS");
                    return false;
                }
            }
        } catch (IOException ex) {
            log.warning("Unable to create file \"" + fil.getAbsolutePath() + "\": " + ex.toString());
            return false;
        }
        try (FileWriter writer = new FileWriter(fil)) {
            writer.write(content);
            res = true;
        } catch (IOException ex) {
            res = false;
            log.warning("Unable to flush content to file \"" + fil.getAbsolutePath() + "\":" + ex.toString());
        }
        return res;
    }

    public List<File> getFiles() {
        ArrayList<File> res = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            Arrays.stream(files).filter(File::isFile).forEach(res::add);
        }
        return res;
    }

    public List<String> getFileNames() {
        ArrayList<String> res = new ArrayList<String>();
        getFiles().forEach(s -> res.add(s.getName()));
        return res;
    }

    public Optional<List<File>> getFiles(String path) {
        List<File> res = null;
        if (path.contains(File.separator)) {
            Optional<HomeWorkTree> tree = getChild(path);
            if (tree.isPresent()) {
                res = tree.get().getFiles();
            }
        } else {
            res = getFiles();
        }
        return Optional.ofNullable(res);
    }

    /**
     * This method will generate a random ID with length len
     * This will look if a file with the name "prefix + ID + suffix" already exists and regenerate if true
     * @param prefix will be included to the file check (not included in result)
     * @param suffix will be appended to the file check (not included in result)
     * @param len length of the generated ID
     * @param maxRecursion this can be used to prevent StackOverflows if a very high number of files are located
     *                     in the current context (Many recursive calls may be needed to generate a nonexistent ID)
     *                     (Any negative value will be infinite)
     * @return Optional of (Random AlphaNumeric String (the ID)) not filled if maxRecursion has reached 0
     */
    public Optional<String> genFreeID(String prefix, String suffix, int len, int maxRecursion, boolean retFullName) {
        if (maxRecursion == 0) {
            return Optional.empty();
        }
        L4YGRandom.initRndIfNotAlready();
        String id = L4YGRandom.genRandomAlphaNumString(len);
        String fileName = prefix + id + suffix;
        if (fileExists(fileName)) {
            return genFreeID(prefix, suffix, len, maxRecursion - 1, retFullName);
        } else {
            if (retFullName) {
                return Optional.of(fileName);
            }
            return Optional.of(id);
        }
    }

    /**
     * Check if a file exists in the folder represented by this tree
     * @param fileName The name to search for
     * @return true if the file was found
     */
    public boolean fileExists(String fileName) {
        Optional<File[]> files = Optional.ofNullable(file.listFiles());
        if (files.isPresent()) {
            for (int I = 0; I < files.get().length; I++) {
                if (files.get()[I].getName().equals(fileName)) {
                    log.finest("HWT#fileExists$" + file.getPath() + ": " + fileName + " -> true");
                    return true;
                }
            }
        }
        log.finest("HWT#fileExists$" + file.getPath() + ": " + fileName + " -> false");
        return false;
    }

    public boolean deleteFile(String name) {
        File fil = new File(file, name);
        //Is not ignored, if deleting fails, exists is still true
        fil.delete();
        return !fil.exists();
    }

}
