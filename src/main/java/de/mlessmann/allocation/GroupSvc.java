package de.mlessmann.allocation;

import de.mlessmann.config.ConfigNode;
import de.mlessmann.homework.HWMgrSvc;
import de.mlessmann.hwserver.HWServer;

import java.util.*;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 02.09.16.
 */
public class GroupSvc {

    private HWServer server;
    private GroupMgrSvc svc;

    private ConfigNode node;

    private Map<String, HWUser> myUsers;

    public GroupSvc(GroupMgrSvc svc, HWServer server) {
        this.server = server;
        this.svc = svc;
        myUsers = new HashMap<String, HWUser>();
    }

    public ConfigNode getNode() { return node; }

    public boolean init(ConfigNode node) {
        myUsers.clear();
        boolean valid =
                node.hasNode("users")
                && node.getNode("users").isHub();
        if (!valid) {
            server.onMessage(this, Level.WARNING, "Attempted to load invalid group!");
            return false;
        }
        Optional<List<String>> oU = node.getNode("users").getKeys();
        if (!oU.isPresent()) {
            server.onMessage(this, Level.SEVERE, "Unable to load myUsers: No keys present!");
            return false;
        }
        List<String> users = oU.get();
        int[] failed = {0};
        users.forEach(s -> {
            server.onMessage(this, Level.FINER, "Loading user \"" + s + '\"');
            if (!loadUser(s, node))
                failed[0]++;
        });
        if (failed[0] == users.size()) {
            return false;
        }
        server.onMessage(this, failed[0] > 0 ? Level.WARNING : Level.FINE, failed[0] + " users failed to load.");
        this.node = node;
        return true;
    }

    private boolean loadUser(String name, ConfigNode node) {
        ConfigNode uNode = node.getNode("users");
        if (!uNode.hasNode(name)){
            server.onMessage(this, Level.WARNING, "Unable to load user \"" + name + "\": Node not found");
            return false;
        }
        HWUser user = new HWUser(this, server);
        if (user.init(uNode.getNode(name))) {
            myUsers.put(user.getUserName(), user);
            return true;
        }
        return false;
    }

    public String getName() {
        return node.getKey();
    }

    public Optional<HWMgrSvc> getHWMgr() {
        return svc.getHWMgrFor(this.getName());
    }

    public Optional<HWUser> getUser(String userName) {
        HWUser u = null;
        if (myUsers.containsKey(userName))
            u = myUsers.get(userName);
        return Optional.ofNullable(u);
    }

    public List<HWUser> getUsers() {
        List<HWUser> l = new ArrayList<>();
        myUsers.forEach((k,v) -> l.add(v));
        return l;
    }

    public boolean createUser(String name) {
        ConfigNode newUser = node.getNode("users", name);
        newUser.getNode("name").setString("default");
        newUser.getNode("auth", "method").setString("default");
        newUser.getNode("auth", "default").setString("default");
        HWPermission.setDefaults(newUser);
        node.getNode("users").delNode("default");
        return true;
    }

    public boolean reload() {
        return init(getNode());
    }
}
