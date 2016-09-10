package de.mlessmann.allocation;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Life4YourGames on 08.06.16.
 */
public class HWPermission {

    public static JSONArray defaultValueSet() {
        JSONArray res = new JSONArray();
        res.put("default");
        res.put(0); //Has - value
        res.put(0); //Allowed to give - value
        res.put(0); //Needed Modify Value

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

    public HWPermission(String name, List<Integer> values) {
        this.name = name;
        valueSet = new JSONArray();
        values.forEach(valueSet::put);
    }

    public HWPermission(String name, int... values) {
        this.name = name;
        valueSet = new JSONArray();
        Arrays.stream(values).forEach(i -> valueSet.put(i));
    }

    public int hasValue() {
        return valueSet.optInt(0, 0);
    }

    public int giveValue() {
        return valueSet.optInt(1, 0);
    }

    public int neededModifyValue() {
        return valueSet.optInt(2, 0);
    }

    public String getName() {
        return name;
    }

    public List<Integer> values() {
        ArrayList<Integer> r = new ArrayList<>();
        for (Object o : valueSet) {
            if (o instanceof Integer)
                r.add((Integer) o);
        }
        return r;
    }

}
