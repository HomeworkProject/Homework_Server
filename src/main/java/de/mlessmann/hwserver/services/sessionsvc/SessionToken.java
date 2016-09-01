package de.mlessmann.hwserver.services.sessionsvc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.LocalDateTime;

/**
 * Created by Life4YourGames on 01.09.16.
 */
public class SessionToken {

    public static final SessionToken INVALID = new SessionToken("NULL", LocalDateTime.MIN);

    public static SessionToken fromJSON(JSONObject o) {

        SessionToken t = INVALID;

        if (o.has("token") && o.has("expires")) {
            Object oToken = o.get("token");
            Object oExp = o.get("expires");
            if ((oToken instanceof String) && (oExp instanceof JSONArray)) {
                String sToken = (String) oToken;
                JSONArray aExp = (JSONArray) oExp;
                if (aExp.length() >= 5) {
                    try {
                        LocalDateTime expires = LocalDateTime.of(
                                aExp.getInt(0),
                                aExp.getInt(1),
                                aExp.getInt(2),
                                aExp.getInt(3),
                                aExp.getInt(4)
                        );
                        t = new SessionToken(sToken, expires);
                    } catch (DateTimeException e) {
                        //Ignore
                    }
                }
            }
        }
        return t;
    }




    private String token = null;
    private LocalDateTime validUntil = null;

    public SessionToken(String token, LocalDateTime validUntil) {
        this.token = token;
        this.validUntil = validUntil;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SessionToken))
            return false;
        SessionToken t = (SessionToken) o;

        return t.getToken().equals(getToken());
    }

    public boolean isValid() {
        return validUntil != null
                && !validUntil.equals(LocalDateTime.MIN)
                && validUntil.isAfter(LocalDateTime.now());
    }

    public JSONObject toJSON() {

        JSONObject o = new JSONObject();
        o.put("token", token);

        JSONArray exp = new JSONArray();
        exp.put(validUntil.getYear());
        exp.put(validUntil.getMonthValue());
        exp.put(validUntil.getDayOfMonth());
        exp.put(validUntil.getHour());
        exp.put(validUntil.getMinute());

        o.put("expires", exp);

        return o;
    }

}
