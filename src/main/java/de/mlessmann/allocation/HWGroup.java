package de.mlessmann.allocation;

import de.mlessmann.config.HWConfig;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.homework.HomeWorkTree;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.perms.Permission;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 04.05.16.
 * @author Life4YourGames
 */
public class HWGroup {

    public static JSONObject getDefaultConfig() {

        JSONObject defaultConfig = new JSONObject();

        defaultConfig.put("type", "config");

        defaultConfig.put("configVersion", HWConfig.confVersion);

        defaultConfig.put("users", new JSONArray());

        return defaultConfig;

    }

    private String gName;
    private String storeFolder;

    private HWServer hwServer;
    private Logger log;

    private boolean initialized;

    private HomeWorkTree hwTree;

    //---Begin users---
    private HWConfig userConf;
    private Map<String, HWUser> users;
    //-----End users---

    public HWGroup (String groupName, HWServer hwserver) {

        gName = groupName;
        storeFolder = (new StringBuilder("groups")
                .append(File.separator)
                .append(gName))
                .toString();
        hwServer = hwserver;

        log = hwserver.getLogger();
        log.finest("Group \"" + gName + "\" created");

        users = new HashMap<String, HWUser>();

    }

    public HWGroup (HWServer hwserver) {

        this("Unnamed" + LocalDateTime.now().toString(), hwserver);

    }

    public void init() {

        hwTree = new HomeWorkTree(hwServer, storeFolder);

        hwTree.analyze();

        File fil = new File(storeFolder + File.separator + "users.conf");

        if (fil.exists()) {

            //TODO: Finish user integration

            userConf = new HWConfig(hwServer);

            userConf.defaultConf = getDefaultConfig();

            userConf.createIfNotFound(fil);

            userConf.open((File) null); // ?! xD

            JSONArray jUsers = userConf.getJSON().getJSONArray("users");

            if (jUsers.length() > 0) {

                for (Object o : jUsers) {

                    HWUser u = new HWUser((JSONObject) o, this);

                    users.put(u.getUserName(), u);

                }

            } else {

                HWUser defUser = HWUser.getDefaultUser(this);

                users.put(defUser.getUserName(), defUser);

            }

        }

    }

    public Optional<HWUser> getUser(String username, String auth) {

        HWUser user = null;

        if (users.containsKey(username)) {

            HWUser u = users.get(username);

            if (u.authenticate(auth)) {

                user = u;

            }

        }

        return Optional.ofNullable(user);

    }

    public synchronized ArrayList<HomeWork> getHWOn(LocalDate date, ArrayList<String> subjectFilter) {

        ArrayList<HomeWork> res = new ArrayList<HomeWork>();

        String subPath = date.getYear() + File.separator + date.getMonthValue() + File.separator + date.getDayOfMonth();

        Optional<HomeWorkTree> tree = hwTree.getChild(subPath);

        if (tree.isPresent()) {

            ArrayList<String> list = tree.get().getFileNames();

            ArrayList<HomeWork> tempRes = res;

            String pathWithoutName = storeFolder + File.separator + subPath + File.separator;

            if (subjectFilter == null || subjectFilter.size() == 0) {
                list.stream()
                        .filter(s -> s.startsWith("hw_"))
                        .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, hwServer)));
            } else {

                list.stream()
                        .filter(s -> s.startsWith("hw_"))
                        .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, hwServer)));

                HomeWork[] arr = (HomeWork[]) res.stream()
                        .filter(hw -> hw.getJSON().has("subject"))
                        .filter(hw -> subjectFilter.contains(hw.getJSON().getString("subject")))
                        .toArray();

                res = new ArrayList<HomeWork>(Arrays.asList(arr));
            }

        }

        return res;

    }

    public synchronized ArrayList<HomeWork> getHWBetween(LocalDate from, LocalDate to, ArrayList<String> subjectFilter, boolean overrideLimit) {

        int daysSearched = 0;
        boolean kill = false;

        ArrayList<HomeWork> res = new ArrayList<HomeWork>();

        log.finest("HWSearch#START # " + from.toString() + " -> " + to.toString());

        for (int cYear = from.getYear(); cYear <= to.getYear() && !kill; cYear++) {

            for (int cMonth = 1; cMonth <= 12 && !kill; cMonth++) {

                if (cYear == from.getYear() && cMonth < from.getMonthValue()) {

                    log.finest("HWSearch#SKIP # MM_" + cYear + '-' + cMonth);

                    continue;
                }
                if (cYear == to.getYear() && cMonth > to.getMonthValue()) {

                    log.finest("HWSearch#BREAK # MM_" + cYear + '-' + cMonth);

                    break;
                }

                for (int cDay = 1; cDay <= 31 && !kill; cDay++) {

                    if (cYear == from.getYear() && cMonth == from.getMonthValue() && cDay < from.getDayOfMonth()) {

                        log.finest("HWSearch#SKIP # dd_" + cYear + '-' + cMonth + '-' + cDay);

                        continue;
                    }
                    if (cYear == to.getYear() && cMonth == to.getMonthValue() && cDay >= to.getDayOfMonth()) {

                        log.finest("HWSearch#BREAK # dd_" + cYear + '-' + cMonth + '-' + cDay);

                        break;
                    }

                    //This will force the search to stop after 64 days have been searched to actively
                    //prevent too many iterations -> Clients should set request ranges reasonably
                    //Of course it can be disabled for internal usage
                    if (!overrideLimit && daysSearched++ >= 64) kill = true;

                    StringBuilder childPath = new StringBuilder()
                            .append(cYear).append(File.separator)
                            .append(cMonth).append(File.separator)
                            .append(cDay);

                    Optional<HomeWorkTree> tree = hwTree.getChild(childPath.toString());

                    if (tree.isPresent()) {

                        ArrayList<String> list = tree.get().getFileNames();

                        String pathWithoutName = storeFolder + File.separator + childPath + File.separator;

                        log.finest("HWSearch#ADD # " + childPath);

                        ArrayList<HomeWork> tempRes = res;

                        if (subjectFilter == null || subjectFilter.size() == 0) {
                            list.stream()
                                    .filter(s -> s.startsWith("hw_"))
                                    .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, hwServer)));
                        } else {

                            list.stream()
                                    .filter(s -> s.startsWith("hw_"))
                                    .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, hwServer)));

                            HomeWork[] arr = (HomeWork[]) res.stream()
                                    .filter(hw -> hw.getJSON().has("subject"))
                                    .filter(hw -> subjectFilter.contains(hw.getJSON().getString("subject")))
                                    .toArray();

                            res = new ArrayList<HomeWork>(Arrays.asList(arr));
                        }

                    } else {

                        log.finest("HWSearch#SKIP # " + childPath + " not present");

                    }

                }

            }

        }

        return res;

    }

    public synchronized int addHW(JSONObject hw, HWUser withUser) {

        JSONArray date = null;

        try {

            date = hw.getJSONArray("date");

        } catch (JSONException ex) {

            log.warning("Reached JSONEx while adding hw -> unchecked parameter passed ?");

            return -1;

        }

        if (date != null && date.length() >= 3) {

            StringBuilder path = new StringBuilder()
                    .append(date.getInt(0)).append(File.separator)
                    .append(date.getInt(1)).append(File.separator)
                    .append(date.getInt(2));

            Optional<HomeWorkTree> tree = hwTree.getOrCreateChild(path.toString());

            if (!tree.isPresent()) {

                log.warning("Unable to add hw: Tree for \"" + path.toString() + "\" is missing!");

                return -1;

            }

            Optional<String> id;

            if (!hw.has("id")) {

                id = tree.get().genFreeID("hw_", ".json", 20, 200, false);

                if (!id.isPresent()) {

                    log.warning("genFreeID reached null for \"" + path.toString() + "\": Max IDs reached ?");

                    return -1;

                }

                String fileName = "hw_" + id.get() + ".json";

                hw.put("id", id);

            } else {

                id = Optional.of(hw.getString("id"));

            }

            String fileName = "hw_" + id.get() + ".json";

            if (withUser != null) {

                boolean e = tree.get().fileExists(fileName);

                int crVal = withUser.getPermissionValue(Permission.HW_ADD_NEW);
                int eVal = withUser.getPermissionValue(Permission.HW_ADD_EDIT);

                if (e && eVal == 0) {
                    return 2;
                }
                if (e && crVal == 0) {
                    return 1;
                }
            }

            boolean res = tree.get().flushOrCreateFile(fileName, hw.toString(2));

            log.finest("HWG#addHW$" + gName + "{FlushRes}: " + fileName + " -> " + res);

            return res ? 0 : -1;

        }

        return -1;

    }

    public synchronized int delHW(LocalDate date, String id, HWUser withUser) {

        String subPath = date.getYear() + File.separator + date.getMonthValue() + File.separator + date.getDayOfMonth();

        Optional<HomeWorkTree> tree = hwTree.getChild(subPath);

        if (!tree.isPresent()) {
            return 0;
        }

        if (withUser != null) {

            int dVal = withUser.getPermissionValue(Permission.HW_DEL);

            if (dVal == 0) return 2;

        }

        return tree.get().deleteFile("hw_" + id + ".json") ? 0 : 1;

    }

}
