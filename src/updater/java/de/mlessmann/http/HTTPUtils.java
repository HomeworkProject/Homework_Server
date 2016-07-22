package de.mlessmann.http;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

/**
 * Created by Life4YourGames on 04.07.16.
 */
public class HTTPUtils {

    public static String GETHTTPSText(String sUrl, int port, String proxy) throws IOException {

        String res = null;

        URL url = new URL(sUrl);

        HttpsURLConnection connection;

        if (proxy == null) {

            connection = (HttpsURLConnection) url.openConnection();

        } else {

            Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy, port));

            connection = (HttpsURLConnection) url.openConnection(p);

            Authenticator.setDefault(new ProxyAuthDummy());

        }

        StringBuilder content = new StringBuilder();

        InputStream input = connection.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        reader.lines().forEach(s -> content.append(s).append("\n"));

        if (content.length() > 1) {

            res = content.toString();

        }

        return res;

    }

    public static String GETHTTPText(String sURL, int port, String proxy) throws IOException {

        String res = null;

        URL url = new URL(sURL);

        URLConnection connection;

        if (proxy == null) {

            connection = url.openConnection();

        } else {

            Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy, port));

            connection = url.openConnection(p);

            Authenticator.setDefault(new ProxyAuthDummy());

        }

        StringBuilder content = new StringBuilder();

        InputStream input = connection.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        reader.lines().forEach(s -> content.append(s).append("\n"));

        if (content.length() > 1) {

            res = content.toString();

        }

        return res;

    }

    public static void HTTPGetFile(String sURL, String targetPath) throws IOException {

        File target = new File(targetPath);

        if (!target.exists()) target.createNewFile();

        URL u = new URL(sURL);

        ReadableByteChannel channel = Channels.newChannel(u.openStream());

        FileOutputStream out = new FileOutputStream(target);

        out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE); //Results in a maximum size of 8 ExaBytes

        channel.close();

        out.close();

    }

}
