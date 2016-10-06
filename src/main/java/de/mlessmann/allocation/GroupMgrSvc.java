package de.mlessmann.allocation;

import de.mlessmann.common.annotations.Nullable;
import de.mlessmann.config.ConfigNode;
import de.mlessmann.config.api.ConfigLoader;
import de.mlessmann.homework.HWMgrSvc;
import de.mlessmann.hwserver.HWServer;

import java.util.*;
import java.util.logging.Level;

import static java.util.logging.Level.FINE;

/**
 * Created by Life4YourGames on 23.08.16.
 */
public class GroupMgrSvc {

    private ConfigLoader configLoader;
    private ConfigNode config;

    private HWServer server;
    private List<GroupSvc> myGroups;
    private Map<String, HWMgrSvc> myHWMgrs;
    public GroupMgrSvc(HWServer server) {
        this.server = server;
        myGroups = new ArrayList<GroupSvc>();
        myHWMgrs = new HashMap<String, HWMgrSvc>();
    }

    public boolean init(ConfigNode conf) {
        myGroups.clear();
        myHWMgrs.clear();

        this.config = conf;

        Optional<List<String>> oGList = config.getKeys();
        List<String> groups;
        if (!oGList.isPresent()) {
            groups = new ArrayList<String>();
        } else {
            groups = oGList.get();
        }

        if (groups.isEmpty()) {
            createDefGroup();
        }
        return loadGroups();
    }

    private void createDefGroup() {
        ConfigNode newGroup = config.getNode("default");

        ConfigNode newUser = newGroup.getNode("users", "default");
        newUser.getNode("name").setString("default");
        newUser.getNode("auth", "method").setString("default");
        newUser.getNode("auth", "default").setString("default");
        HWPermission.setDefaults(newUser);
    }

    private boolean loadGroups() {
        Optional<List<String>> oG = config.getKeys();
        if (!oG.isPresent()) {
            server.onMessage(this, Level.SEVERE, "Unable to load groups: No keys present!");
            return false;
        }
        List<String> groups = oG.get();
        int[] failed = {0};
        groups.forEach(s -> {
            server.onMessage(this, FINE, "Loading group \"" + s + '\"');
            if (!loadGroup(s))
                failed[0]++;
        });
        server.onMessage(this,
                failed[0] == 0 ? FINE : Level.WARNING,
                failed[0] + " groups failed to load."
        );
        return failed[0]!=groups.size();
    }

    public boolean loadGroup(String name) {
        if (!config.hasNode(name)){
            server.onMessage(this, Level.WARNING, "Unable to load group \"" + name + "\": Config not found");
            return false;
        }
        GroupSvc group = getGroupNullable(name);
        if (group!=null) {
            server.onMessage(this, FINE, "Reloading group \"" + name + '\"');
            return group.init(group.getNode());
        }
        group = new GroupSvc(this, server);
        if (group.init(config.getNode(name))) {
            myGroups.add(group);
            return true;
        }
        return false;
    }

    public boolean createGroup(String name) {
        ConfigNode newGroup = config.getNode(name);

        ConfigNode newUser = newGroup.getNode("users", "default");
        newUser.getNode("name").setString("default");
        newUser.getNode("auth", "method").setString("default");
        newUser.getNode("auth", "default").setString("default");
        HWPermission.setDefaults(newUser);
        return true;
    }

    public synchronized Optional<HWMgrSvc> getHWMgrFor(String groupName) {
        if (myHWMgrs.containsKey(groupName)) {
            return Optional.of(myHWMgrs.get(groupName));
        }
        HWMgrSvc hwSvc = new HWMgrSvc(this, server);
        if (!hwSvc.init("groups/" + groupName)) {
            server.onMessage(this, Level.SEVERE, "Unable to initialize HWMgrSvc for " + groupName);
            return Optional.empty();
        }
        hwSvc.setGroupName(groupName);
        myHWMgrs.put(groupName, hwSvc);
        return Optional.of(hwSvc);
    }

    public synchronized List<GroupSvc> getGroups() {
        return myGroups;
    }

    public synchronized Optional<GroupSvc> getGroup(String name) {
        return Optional.ofNullable(getGroupNullable(name));
    }

    @Nullable
    public synchronized GroupSvc getGroupNullable(String name) {
        GroupSvc[] g = {null};
        myGroups.stream().filter(grp -> grp.getName().equals(name)).forEach(grp2 -> g[0] = grp2);
        return g[0];
    }

}
