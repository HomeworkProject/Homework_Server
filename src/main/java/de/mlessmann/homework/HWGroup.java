package de.mlessmann.homework;

import de.mlessmann.hwserver.HWServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Life4YourGames on 04.05.16.
 * @author Life4YourGames
 */
public class HWGroup {

    private String gName;
    private String storeFolder;
    private HWServer hwServer;
    private Logger log;
    private boolean initialized;
    private HomeWorkTree hwTree;

    public HWGroup (String groupName, HWServer hwserver) {

        gName = groupName;
        storeFolder = (new StringBuilder("groups")
                .append(File.separator)
                .append(gName))
                .toString();
        hwServer = hwserver;

        log = hwserver.getLogger();
        log.finest("Group \"" + gName + "\" created");

    }

    public HWGroup (HWServer hwserver) {

        this("Unnamed" + LocalDateTime.now().toString(), hwserver);

    }

    public void init() {

        hwTree = new HomeWorkTree(hwServer, storeFolder);

        hwTree.analyze();

    }

    public synchronized ArrayList<HomeWork> getHWBetween(LocalDate from, LocalDate to, boolean overrideLimit) {

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

                        list.stream()
                                .filter(s -> s.startsWith("hw_"))
                                .forEach(name -> res.add(HomeWork.newByPath(pathWithoutName + name, hwServer)));

                    } else {

                        log.finest("HWSearch#SKIP # " + childPath + " not present");

                    }

                }

            }

        }

        return res;

    }

    public synchronized boolean addHW(JSONObject hw) {

        int yyyy = 0;
        int MM = 0;
        int dd = 0;

        try {

            yyyy = hw.getInt("yyyy");
            MM = hw.getInt("MM");
            dd = hw.getInt("dd");

        } catch (JSONException ex) {

            log.warning("Reached JSONEx while adding hw -> unchecked parameter passed ?");

            return false;

        }

        if (yyyy != 0 && MM != 0 && dd != 0) {

            StringBuilder path = new StringBuilder()
                    .append(yyyy).append(File.separator)
                    .append(MM).append(File.separator)
                    .append(dd);

            Optional<HomeWorkTree> tree = hwTree.getOrCreateChild(path.toString());

            if (!tree.isPresent()) {

                log.warning("Unable to add hw: Tree for \"" + path.toString() + "\" is missing!");

                return false;

            }

            Optional<String> fileName = tree.get().genFreeID("hw_", ".json", 20, 200, true);

            if (!fileName.isPresent()) {

                log.warning("genFreeID reached null for \"" + path.toString() + "\": Max IDs reached ?");

                return false;

            }

            boolean res = tree.get().flushOrCreateFile(fileName.get(), hw.toString(2));

            log.finest("HWG#addHW$" + gName + "{FlushRes}: " + fileName.get() + " -> " + res);

            return res;

        }

        return false;

    }

}
