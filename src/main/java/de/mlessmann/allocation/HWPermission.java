package de.mlessmann.allocation;

import de.mlessmann.config.ConfigNode;
import de.mlessmann.hwserver.HWServer;
import de.mlessmann.perms.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Life4YourGames on 08.06.16.
 */
public class HWPermission {

    public static final Integer[] INITVALS = new Integer[]{0,0,0};
    public static List<Integer> INITVALUES() {
        return Arrays.asList(INITVALS);
    }

    public static void setDefaults(ConfigNode userNode) {
        ConfigNode pNode = userNode.getNode("permissions");
        Permission.defPerms().forEach(s -> {
            pNode.getNode(s).setList(INITVALUES());
        });
    }

    private HWUser user;
    private HWServer server;

    private ConfigNode node;

    public HWPermission(HWUser user, HWServer server) {
        this.user = user;
    }

    public boolean readFrom(ConfigNode node) {
        boolean valid =
                node.hasNode("values")
                && node.getNode("values").isType(List.class);
        if (!valid) {
            return false;
        }
        this.node = node;
        return true;
    }

    public ConfigNode initFrom(String name, int[] values) {
        node = new ConfigNode(null, name);
        ArrayList<Integer> l = new ArrayList<Integer>();
        Arrays.stream(values).forEach(l::add);
        node.getNode("values").setList(l);
        return node;
    }

    public int getValue(int index) {
        List<?> l = node.getNode("values").getList();
        if (index >= l.size()) {
            server.onMessage(this, Level.INFO,
                    "Some permissions are not set correctly: Index " + index + "is o.o.b.!");
            return 0;
        }
        Object o = l.get(index);
        if (!(o instanceof Integer)) {
            server.onMessage(this, Level.INFO,
                    "Some permissions are not set correctly: Index " + index + " is not of type Int!");
            return 0;
        }
        return ((Integer) o);
    }

    public String getName() {
        return node.getKey();
    }

    public ConfigNode getNode() { return node; }
}
