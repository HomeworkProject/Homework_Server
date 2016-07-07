package de.mlessmann.allocation;

import de.mlessmann.authentication.IAuthMethod;
import de.mlessmann.homework.HomeWork;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Life4YourGames on 08.06.16.
 */
public class HWUser {

    public static HWUser getDefaultUser(HWGroup g) {

        JSONObject obj = new JSONObject();

        JSONArray perms = new JSONArray();

        obj.put("permissions", perms);
        obj.put("name", DEFNAME);
        obj.put("password", DEFPASS);
        obj.put("authMethod", DEFAUTH);

        return new HWUser(obj, g);

    }

    public static final String DEFNAME = "default";
    public static final String DEFPASS = "default";
    public static final String DEFAUTH = "default";


    //Authentication
    private String authMethod = DEFAUTH;
    private String userName = DEFNAME;
    private String passData = DEFPASS;
    private IAuthMethod myAuthMethod;

    private Map<String, HWPermission> permissions;

    private JSONObject json;

    private HWGroup myGroup;

    public HWUser(JSONObject obj, HWGroup group) {

        permissions = new HashMap<String, HWPermission>();

        json = obj;

        if (json.has("permissions")) {

            JSONArray arr = json.getJSONArray("permissions");

            for (Object o : arr) {

                HWPermission perm = new HWPermission((JSONArray) o);

            }

        }

        userName = json.getString("name");
        passData = json.getString("password");
        myGroup = group;

        if (json.has("authMethod")) {

            authMethod = json.getString("authMethod");

            Optional<IAuthMethod> m = myGroup.getHwServer().getAuthProvider().getMethod(authMethod);

            if (m.isPresent()) {
                myAuthMethod = m.get();
            } else {
                authMethod = "default";
                myAuthMethod = myGroup.getHwServer().getAuthProvider().getDefault();
                myGroup.getLogger().warning("User \"" + userName + "\" wants an invalid authMethod: \"" + authMethod + "\" using \"" + getAuthIdent() + "\"");
            }

        }

    }

    public String getUserName() {

        return userName;

    }

    public String getPassData() {

        return passData;

    }

    public boolean authenticate(String auth) {

        return myAuthMethod.authorize(passData, auth);

    }

    public int getPermissionValue(String permissionName) {

        if (permissions.containsKey(permissionName)) {

            return permissions.get(permissionName).hasValue();

        }

        return HWPermission.DEFAULT.hasValue();

    }

    public void addPermission(HWPermission perm) {

        permissions.put(perm.getName(), perm);

    }

    public int addHW(JSONObject obj) {

        return myGroup.addHW(obj, this);

    }

    public ArrayList<HomeWork> getHWOn(LocalDate date, ArrayList<String> subjectFilter) {

        return myGroup.getHWOn(date, subjectFilter);

    }

    public ArrayList<HomeWork> getHWBetween(LocalDate from, LocalDate to, ArrayList<String> subjectFilter, boolean overrideLimit) {

        return myGroup.getHWBetween(from, to, subjectFilter, overrideLimit);

    }

    public int delHW(LocalDate date, String id) {

        return myGroup.delHW(date, id, this);

    }

    public IAuthMethod getAuth() { return myAuthMethod; }

    public String getAuthIdent() { return myAuthMethod.getIdentifier(); }

    public JSONObject getJSON() { return json; }

}