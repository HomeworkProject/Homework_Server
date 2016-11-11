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
    private LocationType type = LocationType.INVALID;

    public HWAttachmentLocation(JSONObject location) {
        if (location != null) {
            String webLocation = location.optString("url");
            if (webLocation != null) {
                remoteURL = webLocation;
                type = LocationType.WEB;
            } else {
                String hwID = location.optString("ownerhw");
                JSONArray hwDate = location.optJSONArray("date");
                String id = location.optString("id");
                if (hwID != null && id != null && hwDate != null && hwDate.length() >= 3) {
                    try {
                        date = LocalDate.of(hwDate.getInt(0), hwDate.getInt(1), hwDate.getInt(2));
                        this.hwID = hwID;
                        this.assetID = id;
                        this.type = LocationType.SERVER;
                    } catch (Exception e) {
                        //Simple: still invalid
                    }
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

    public LocationType getType() {
        return type;
    }

    public enum LocationType {
        WEB,
        SERVER,
        INVALID
    }
}
