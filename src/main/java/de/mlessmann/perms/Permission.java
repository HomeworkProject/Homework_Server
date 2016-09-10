package de.mlessmann.perms;

import de.mlessmann.allocation.HWPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Life4YourGames on 17.06.16.
 */
public class Permission {

    /**
     * Permission for adding a new HomeWork
     */
    public static String HW_ADD_NEW = "hw_add";

    /**
     * Permission for editing an existing HomeWork
     */
    public static String HW_ADD_EDIT =  "hw_edit";

    /**
     * Permission for deleting an existing HomeWork
     */
    public static String HW_DEL = "hw_del";

    /**
     * Permission to manage users
     * (Remember that values really matter here)
     */
    public static String USR_MANAGE = "usr_manage";

    /**
     * Permission to manage a group
     * (Remember that values really matter here)
     */
    public static String GROUP_MANAGE = "grp_manage";

    /**
     * Permission to manage the instance
     * (Remember that values really matter here)
     */
    public static String INSTANCE_MANAGE = "inst_manage";

    /**
     * Permission to manage the whole process (all instances)
     *
     */
    public static String PROCESS_MANAGE = "proc_manage";

    public static List<HWPermission> adminDefaults = new ArrayList<HWPermission>();
    public static List<HWPermission> defaults = new ArrayList<HWPermission>();

    public static void nativeInitDefaults() {
        defaults.add(new HWPermission(HW_ADD_NEW, 1, 0 , 1));
        defaults.add(new HWPermission(HW_ADD_EDIT, 1, 0, 1));
        defaults.add(new HWPermission(HW_DEL, 1, 0, 1));

        adminDefaults.add(new HWPermission(HW_ADD_NEW, 100, 90 , 101));
        adminDefaults.add(new HWPermission(HW_ADD_EDIT, 100, 90, 101));
        adminDefaults.add(new HWPermission(HW_DEL, 100, 90, 101));
        adminDefaults.add(new HWPermission(USR_MANAGE, 100, 90, 101));
        adminDefaults.add(new HWPermission(GROUP_MANAGE, 100, 90, 101));
        adminDefaults.add(new HWPermission(INSTANCE_MANAGE, 100, 90, 101));
        adminDefaults.add(new HWPermission(PROCESS_MANAGE, 100, 90, 101));
    }

}
