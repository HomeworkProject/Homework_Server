package de.mlessmann.allocation;

import de.mlessmann.authentication.IAuthMethod;
import de.mlessmann.common.annotations.Nullable;
import de.mlessmann.config.ConfigNode;
import de.mlessmann.homework.HWMgrSvc;
import de.mlessmann.homework.HomeWork;
import de.mlessmann.hwserver.HWServer;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 08.06.16.
 */
public class HWUser {

    public static final String DEFNAME = "default";
    public static final String DEFPASS = "default";

    //Authentication
    private String userName = DEFNAME;
    private String authData = DEFPASS;
    private IAuthMethod authMethod;

    private List<HWUserListener> listeners;

    private ConfigNode node;

    private GroupSvc group;
    private HWServer server;

    public HWUser(GroupSvc group, HWServer server) {
        this.listeners = new ArrayList<HWUserListener>();
        this.group = group;
        this.server = server;
    }

    public boolean init(ConfigNode node) {

        boolean valid = true;
        //Should we set up this user?
        if (node.hasNode("onLoad")) {
            ConfigNode onLoad = node.getNode("onLoad");
            if (onLoad.hasNode("passwd")) {
                String authID = onLoad.getNode("passwd", "method").optString("default");
                String passwd = onLoad.getNode("passwd", "password").optString("null");
                valid = valid & setAuthInfo(authID, passwd, node.getNode("auth"));
            }
            if (onLoad.hasNode("perm")) {
                //Set permissions
                if (onLoad.getNode("perm").optString("admin").equals("admin")) {
                    HWPermission.setAdminDefaults(node);
                } else {
                    HWPermission.setDefaults(node);
                }
            }
            //Delete processed node
            node.delNode("onLoad");
        }
        valid = valid &&
                !node.getKey().isEmpty()
                && node.hasNode("auth")
                && node.getNode("auth").isHub()
                && node.getNode("auth").hasNode("method")
                && node.getNode("auth", "method").isType(String.class)
                && node.hasNode("permissions")
                && node.getNode("permissions").isHub();

        if (!valid) {
            server.onMessage(this, Level.FINEST, "Invalid User: cannot load!");
            return false;
        }
        Optional<IAuthMethod> m = server.getAuthProvider().getMethod(node.getNode("auth", "method").getString());
        if (!m.isPresent()) {
            server.onMessage(this, Level.SEVERE, "Unable to initialize user \"" + node.getKey()
                    + "\": Auth method not present!");
            return false;
        }
        authMethod = m.get();
        this.node = node;
        notifyOnChange();
        return true;
    }

    public String getUserName() {
        return node.getKey();
    }

    public String getAuthData() {
        return node.getNode("auth", "pass").getString();
    }

    public boolean setAuthInfo(String method, String plaintextPW, @Nullable ConfigNode onNode) {
        ConfigNode n = onNode == null ? node.getNode("auth") : onNode;

        Optional<IAuthMethod> optM = server.getAuthProvider().getMethod(method);
        if (!optM.isPresent()) {
            return false;
        }
        IAuthMethod m = optM.get();

        n.getNode("method").setString(method);
        n.getNode("pass").setString(m.masqueradePass(plaintextPW));
        if (n == node) {
            authMethod = m;
            notifyOnChange();
        }
        return true;
    }

    public boolean authorize(String auth) {
        return authMethod.authorize(getAuthData(), auth);
    }

    public Optional<HWPermission> getPermission(String permissionName) {
        ConfigNode perms = node.getNode("permissions");
        HWPermission perm = null;
        if (perms.hasNode(permissionName)) {
            HWPermission p = new HWPermission(this, server);
            if (p.readFrom(perms.getNode(permissionName)))
                perm = p;
        }
        return Optional.ofNullable(perm);
    }

    public void addPermission(HWPermission perm) {
        node.getNode("permissions").addNode(perm.getNode());
        notifyOnChange();
    }

    public void removePermission(HWPermission perm) {
        node.getNode("permissions").delNode(perm.getNode().getKey());
        notifyOnChange();
    }

    public int addHW(JSONObject obj) {
        Optional<HWMgrSvc> svc = group.getHWMgr();
        if (svc.isPresent()) {
            return svc.get().addHW(obj, this);
        } else {
            server.onMessage(this, Level.SEVERE, "Unable to add HW: No HWMgr found!");
            return -1;
        }
    }

    public int editHW(LocalDate oldDate, String oldID, JSONObject newHW) {
        Optional<HWMgrSvc> svc = group.getHWMgr();
        if (svc.isPresent()) {
            return svc.get().editHW(oldDate, oldID, newHW, this);
        } else {
            server.onMessage(this, Level.SEVERE, "Unable to edit HW: No HWMgr found!");
            return -1;
        }
    }

    public Optional<HomeWork> getHW(int yyyy, int MM, int dd, String hwID) {
        Optional<HWMgrSvc> svc = group.getHWMgr();
        if (svc.isPresent()) {
            return svc.get().getHW(yyyy, MM, dd, hwID);
        } else {
            server.onMessage(this, Level.SEVERE, "Unable to search for HW: No HWMgr found!");
            return Optional.empty();
        }
    }

    public ArrayList<HomeWork> getHWOn(LocalDate date, ArrayList<String> subjectFilter) {
        Optional<HWMgrSvc> svc = group.getHWMgr();
        if (svc.isPresent()) {
            return svc.get().getHWOn(date, subjectFilter);
        } else {
            server.onMessage(this, Level.SEVERE, "Unable to search for HW: No HWMgr found!");
            return new ArrayList<>();
        }
    }

    public ArrayList<HomeWork> getHWBetween(LocalDate from, LocalDate to, ArrayList<String> subjectFilter, boolean overrideLimit) {
        Optional<HWMgrSvc> svc = group.getHWMgr();
        if (svc.isPresent()) {
            return svc.get().getHWBetween(from, to, subjectFilter, overrideLimit);
        } else {
            server.onMessage(this, Level.SEVERE, "Unable to search for HW: No HWMgr found!");
            return new ArrayList<>();
        }
    }

    public int delHW(LocalDate date, String id) {
        Optional<HWMgrSvc> svc = group.getHWMgr();
        if (svc.isPresent()) {
            return svc.get().delHW(date, id, this);
        } else {
            server.onMessage(this, Level.SEVERE, "Unable to delete HW: No HWMgr found!");
            return -1;
        }
    }

    public boolean isValid() {
        //TODO: NEW USER CLASS
        return false;
    }

    public IAuthMethod getAuth() { return authMethod; }

    public String getAuthIdent() { return authMethod.getIdentifier(); }

    private void notifyOnChange() {
        for (int i = listeners.size()-1; i>=0; i--)
            listeners.get(i).onChange(this);
    }

    public void registerListener(HWUserListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void unregisterListener(HWUserListener listener) {
        if (listeners.contains(listener)) listeners.remove(listener);
    }
}