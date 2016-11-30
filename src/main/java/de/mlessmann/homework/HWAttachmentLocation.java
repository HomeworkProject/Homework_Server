package de.mlessmann.homework;

import de.mlessmann.common.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;

/**
 * Created by Life4YourGames on 09.11.16.
 */
public class HWAttachmentLocation {

    private LocalDate date;
    private String hwID;
    private String assetID;
    private String remoteURL;
    private String name = null;
    private boolean virtual = false;
    private LocationType type = LocationType.INVALID;

    public HWAttachmentLocation(JSONObject location) {
        if (location == null)return;
        hwID = location.optString("ownerhw", null);
        name = location.optString("name", null);
        JSONArray hwDate = location.optJSONArray("date");
        try {
            date = LocalDate.of(hwDate.getInt(0), hwDate.getInt(1), hwDate.getInt(2));
        } catch (Exception e) {
            //Simple: still invalid
            return;
        }

        if (hwID != null && name!=null) {
            String webLocation = location.optString("url", null);
            if (webLocation != null) {
                remoteURL = webLocation;
                type = LocationType.WEB;
            } else {
                String id = location.optString("id", null);
                virtual = location.has("virtual");
                if (id != null || virtual) {
                    this.assetID = id;
                    type = LocationType.SERVER;
                }
            }
        }
    }

    @Nullable
    public String getURL() {
        return remoteURL;
    }

    @Nullable
    public LocalDate getDate() {
        return date;
    }

    @Nullable
    public String getHWID() {
        return hwID;
    }

    @Nullable
    public String getAssetID() {
        return assetID;
    }

    @Nullable
    public String getName() { return name; }

    public LocationType getType() {
        return type;
    }

    public boolean isVirtual() { return virtual; }

    public enum LocationType {
        WEB,
        SERVER,
        INVALID
    }
}
