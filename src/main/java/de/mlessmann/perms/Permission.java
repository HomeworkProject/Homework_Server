package de.mlessmann.perms;

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


    /**
     * Some indices
     */
    public static int HASVALUE = 0;
    public static int GIVEVALUE = 1;
    public static int HASTOHAVETOCHANGE = 2;

    public static List<String> defPerms() {
        List<String> l = new ArrayList<String>();
        l.add(HW_ADD_EDIT);
        l.add(HW_ADD_NEW);
        l.add(HW_DEL);
        return l;
    }

}
