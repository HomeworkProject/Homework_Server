package de.mlessmann.homework;

import de.mlessmann.allocation.GroupMgrSvc;
import de.mlessmann.allocation.HWPermission;
import de.mlessmann.allocation.HWUser;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.perms.Permission;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 02.09.16.
 */
public class HWMgrSvc {

    private HWServer server;
    private Logger logger;
    private GroupMgrSvc groupMgrSvc;
    private String group;

    private String storeFolder;
    private HomeWorkTree parentDir;

    public HWMgrSvc(GroupMgrSvc groupMgrSvc, HWServer server) {
        this.server = server;
        this.logger = server.getLogger();
        this.groupMgrSvc = groupMgrSvc;
    }

    public void setGroupName(String name) {
        this.group = name;
    }

    public boolean init(String directory) {
        storeFolder = directory;
        parentDir = new HomeWorkTree(server, directory);
        parentDir.analyze();
        return true;
    }

    //----------------------------------- IMPORTED FROM HWGroup --------------------------------------------------------

    public synchronized ArrayList<HomeWork> getHWOn(LocalDate date, ArrayList<String> subjectFilter) {

        ArrayList<HomeWork> res = new ArrayList<HomeWork>();

        String subPath = date.getYear() + File.separator + date.getMonthValue() + File.separator + date.getDayOfMonth();

        Optional<HomeWorkTree> tree = parentDir.getChild(subPath);

        if (tree.isPresent()) {

            ArrayList<String> list = tree.get().getFileNames();

            ArrayList<HomeWork> tempRes = res;

            String pathWithoutName = storeFolder + File.separator + subPath + File.separator;

            if (subjectFilter == null || subjectFilter.size() == 0) {
                list.stream()
                        .filter(s -> s.startsWith("hw_"))
                        .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, server)));
            } else {

                list.stream()
                        .filter(s -> s.startsWith("hw_"))
                        .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, server)));

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

        logger.finest("HWSearch#START # " + from.toString() + " -> " + to.toString());

        for (int cYear = from.getYear(); cYear <= to.getYear() && !kill; cYear++) {

            for (int cMonth = 1; cMonth <= 12 && !kill; cMonth++) {

                if (cYear == from.getYear() && cMonth < from.getMonthValue()) {

                    logger.finest("HWSearch#SKIP # MM_" + cYear + '-' + cMonth);

                    continue;
                }
                if (cYear == to.getYear() && cMonth > to.getMonthValue()) {

                    logger.finest("HWSearch#BREAK # MM_" + cYear + '-' + cMonth);

                    break;
                }

                for (int cDay = 1; cDay <= 31 && !kill; cDay++) {

                    if (cYear == from.getYear() && cMonth == from.getMonthValue() && cDay < from.getDayOfMonth()) {

                        logger.finest("HWSearch#SKIP # dd_" + cYear + '-' + cMonth + '-' + cDay);

                        continue;
                    }
                    if (cYear == to.getYear() && cMonth == to.getMonthValue() && cDay >= to.getDayOfMonth()) {

                        logger.finest("HWSearch#BREAK # dd_" + cYear + '-' + cMonth + '-' + cDay);

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

                    Optional<HomeWorkTree> tree = parentDir.getChild(childPath.toString());

                    if (tree.isPresent()) {

                        ArrayList<String> list = tree.get().getFileNames();

                        String pathWithoutName = storeFolder + File.separator + childPath + File.separator;

                        logger.finest("HWSearch#ADD # " + childPath);

                        ArrayList<HomeWork> tempRes = res;

                        if (subjectFilter == null || subjectFilter.size() == 0) {
                            list.stream()
                                    .filter(s -> s.startsWith("hw_"))
                                    .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, server)));
                        } else {

                            list.stream()
                                    .filter(s -> s.startsWith("hw_"))
                                    .forEach(name -> tempRes.add(HomeWork.newByPath(pathWithoutName + name, server)));

                            HomeWork[] arr = (HomeWork[]) res.stream()
                                    .filter(hw -> hw.getJSON().has("subject"))
                                    .filter(hw -> subjectFilter.contains(hw.getJSON().getString("subject")))
                                    .toArray();

                            res = new ArrayList<HomeWork>(Arrays.asList(arr));
                        }

                    } else {

                        logger.finer("HWSearch#SKIP # " + childPath + " not present");

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
            logger.warning("Reached JSONEx while adding hw -> unchecked parameter passed ?");
            return -1;
        }

        if (date != null && date.length() >= 3) {

            StringBuilder path = new StringBuilder()
                    .append(date.getInt(0)).append(File.separator)
                    .append(date.getInt(1)).append(File.separator)
                    .append(date.getInt(2));

            Optional<HomeWorkTree> tree = parentDir.getOrCreateChild(path.toString());

            if (!tree.isPresent()) {
                logger.warning("Unable to add hw: Tree for \"" + path.toString() + "\" is missing!");
                return -1;
            }

            Optional<String> id;

            if (!hw.has("id")) {
                id = tree.get().genFreeID("hw_", ".json", 20, 200, false);
                if (!id.isPresent()) {
                    logger.warning("genFreeID reached null for \"" + path.toString() + "\": Max IDs reached ?");
                    return -1;
                }
                String fileName = "hw_" + id.get() + ".json";
                hw.put("id", id.get());
            } else {
                id = Optional.of(hw.getString("id"));
            }

            String fileName = "hw_" + id.get() + ".json";
            if (withUser != null) {
                boolean e = tree.get().fileExists(fileName);
                Optional<HWPermission> optCr = withUser.getPermission(Permission.HW_ADD_NEW);
                int crVal = optCr.isPresent() ? optCr.get().getValue(Permission.HASVALUE) : 0;
                Optional<HWPermission> optE = withUser.getPermission(Permission.HW_ADD_EDIT);
                int eVal = optE.isPresent() ? optE.get().getValue(Permission.HASVALUE) : 0;
                if (e && eVal == 0) {
                    return 2;
                }
                if (e && crVal == 0) {
                    return 1;
                }
            }

            boolean res = tree.get().flushOrCreateFile(fileName, hw.toString(2));
            logger.finest("HWG#addHW$" + group + "{FlushRes}: " + fileName + " -> " + res);
            return res ? 0 : -1;
        }
        return -1;
    }

    public synchronized int delHW(LocalDate date, String id, HWUser withUser) {
        String subPath = date.getYear() + File.separator + date.getMonthValue() + File.separator + date.getDayOfMonth();
        Optional<HomeWorkTree> tree = parentDir.getChild(subPath);
        if (!tree.isPresent()) {
            return 0;
        }
        if (withUser != null) {
            Optional<HWPermission> optD = withUser.getPermission(Permission.HW_DEL);
            int dVal = optD.isPresent() ? optD.get().getValue(Permission.HASVALUE) : 0;
            if (dVal == 0) return 2;
        }
        return tree.get().deleteFile("hw_" + id + ".json") ? 0 : 1;
    }

}
