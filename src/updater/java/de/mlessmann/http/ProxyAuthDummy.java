package de.mlessmann.http;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Created by Life4YourGames on 04.07.16.
 */
public class ProxyAuthDummy extends Authenticator{

    private String user = "";
    private String pass = "";

    @Override
    public PasswordAuthentication getPasswordAuthentication() {

        return new PasswordAuthentication(user, pass.toCharArray());

    }

    public void setUser(String u) {

        user = u;

    }

    public void setPass(String p) {

        pass = p;

    }

}
