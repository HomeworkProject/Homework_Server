package de.mlessmann.homework;

import de.mlessmann.common.annotations.Nullable;
import org.json.JSONObject;

/**
 * Created by Life4YourGames on 07.11.16.
 */
public class HWAttachment {

    private JSONObject obj;
    private HomeWork hw;

    public HWAttachment(JSONObject obj, HomeWork hw) {
        this.obj = obj;
        this.hw = hw;
    }

    @Nullable
    public String getTitle() {
        return obj.optString("title");
    }

    @Nullable
    public String getDescription() {
        return obj.optString("desc");
    }

    public HWAttachmentLocation getLocation() { return new HWAttachmentLocation(obj.optJSONObject("location")); }

    public String getID() {
        return obj.optString("id", "null");
    }

    public JSONObject getJSON() {
        return obj;
    }

}
