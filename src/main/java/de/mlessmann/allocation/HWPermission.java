package de.mlessmann.allocation;

import org.json.JSONArray;

/**
 * Created by Life4YourGames on 08.06.16.
 */
public class HWPermission {

    public static JSONArray defaultValueSet() {

        JSONArray res = new JSONArray();
        res.put("default");
        res.put(0); //Has - value
        res.put(0); //Allowed to give - value

        return res;

    }

    public static final HWPermission DEFAULT = new HWPermission(HWPermission.defaultValueSet());

    //-------------------

    private String name;
    private JSONArray valueSet;

    public HWPermission(JSONArray values) {

        name = values.getString(0);

        valueSet = new JSONArray();
        for (int i = 1; i < values.length(); i++) {

            int v = values.getInt(i);

                valueSet.put(v);

        }

    }

    public int hasValue() {

        if (valueSet.length() < 1) valueSet.put(0);

        return valueSet.getInt(0);

    }

    public int giveValue() {

        if (valueSet.length() < 2) {
            valueSet.put(0);
            valueSet.put(0);
        }

        return valueSet.getInt(1);

    }

    public String getName() {

        return name;

    }

}
