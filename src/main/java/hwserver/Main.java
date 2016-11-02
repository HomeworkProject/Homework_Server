package hwserver;

import de.mlessmann.common.apparguments.AppArgument;
import de.mlessmann.config.ConfigNode;
import de.mlessmann.config.except.RootMustStayHubException;
import de.mlessmann.hwserver.HWServer;
import org.reflections.adapters.JavassistAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class Main {

    public static final String CONSDIV = "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++";

    public static void main (String[] args) throws IOException {

        try {
            JavassistAdapter.includeInvisibleTag = false;

            ArrayList<AppArgument> aa = AppArgument.fromArray(args);

            HWServer hwServer = new HWServer();

            hwServer.preInitialize();

            aa.forEach(hwServer::setArg);

            hwServer.initialize();

            hwServer.start();

        } catch (Exception e) {

            e.printStackTrace();
            try (PrintWriter w = new PrintWriter(new FileWriter("hwserver_crash.log"))) {
                w.println(e.toString());
                e.printStackTrace(w);
                Logger.getGlobal().severe("An unhandled exception occurred! The process is forced to halt!");
                Logger.getGlobal().severe("A crash log has been written to \"hwserver_crash.log\"");
            } catch (IOException e1) {

                Logger.getGlobal().severe("Unable to write crash log: " + e1.toString());
                e1.printStackTrace();

            }
            System.exit(1);
        }
    }

    public static boolean askForConfirm(BufferedReader reader) throws IOException {
        while (true) {
            String l = reader.readLine();
            switch (l) {
                case "Y":
                case "y": return true;
                case "N":
                case "n": return false;
            }
        }
    }

    public static void setupConfiguration(ConfigNode config) {
        boolean exitConfigurator = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        PrintStream out = System.out;
        while (!exitConfigurator) {

            try {
                out.println(CONSDIV);
                out.println("HWServer setup:");
                out.println("[0] Exit");
                out.println("[1] Edit groups");
                out.println("[2] Edit server configuration");
                out.println(CONSDIV);

                try {
                    String l = reader.readLine();

                    Integer i = Integer.parseInt(l);
                    switch (i) {
                        case 0: exitConfigurator = true; break;
                        case 1: editGroup(config, reader); break;
                        case 2: editConfig(config, reader); break;
                        default:
                            out.println("Invalid number: Use 0-2");
                            Thread.sleep(1000);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Unable to read input", e);
                } catch (NumberFormatException e) {
                    out.println("Invalid number, try again");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                out.println("Interrupted!");
            }
        }

        /*
        Node to self: Don't ever do this xD
        Closes inputStream too
        try {
            reader.close();
        } catch (IOException e) {
        }
        TODO: Find another solution to read input in different sections of the server
        */
    }

    private static void editGroup(ConfigNode config, BufferedReader reader) throws IOException {
        PrintStream out = System.out;
        String l;

        out.println("Enter group name:");
        String name = reader.readLine();
        if (name.isEmpty() || name.equals("\n")) {
            out.println("Invalid group name");
        }

        ConfigNode node = config.getNode("groups", name);
        if (node.isVirtual()) {
            out.println("Group doesn't exist. Create? [y/n]");
            if (askForConfirm(reader))
                node.getNode("name").setString(name);
            else
                return;
        }

        boolean exitGrpConfigurator = false;
        while (!exitGrpConfigurator) {
            try {
                out.println(CONSDIV);
                out.println("Editing group:" + name);
                out.println("[0] Exit");
                out.println("[1] Edit users");
                out.println("[2] Delete group");
                out.println(CONSDIV);

                try {
                    l = reader.readLine();

                    Integer i = Integer.parseInt(l);
                    switch (i) {
                        case 0: exitGrpConfigurator = true; break;
                        case 1: editUser(config, name, reader); break;
                        case 2:
                            out.println("Really delete group: " + name);
                            if (askForConfirm(reader)) {
                                config.getNode("groups").delNode(name);
                                out.println("Deleted.");
                                return;
                            }
                        default:
                            out.println("Invalid number: Use 0-2");
                            Thread.sleep(1000);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Unable to read input", e);
                } catch (NumberFormatException e) {
                    out.println("Invalid number, try again");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                out.println("Interrupted!");
            }
        }
    }

    private static void editUser(ConfigNode config, String groupName, BufferedReader reader) throws IOException {
        PrintStream out = System.out;
        String l;

        out.println("Enter user name:");
        String name = reader.readLine();
        if (name.isEmpty() || name.equals("\n")) {
            out.println("Invalid user name");
        }

        ConfigNode node = config.getNode("groups", groupName, "users", name);
        if (node.isVirtual()) {
            out.println("User doesn't exist. Create? [y/n]");
            if (askForConfirm(reader)) {
                node.getNode("name").setString(name);
                node.getNode("auth", "method").setString("default");
                node.getNode("auth", "default").setString("default");
                out.println("Is this an administrative user ?[y/n]");
                if (!askForConfirm(reader)) {
                    node.getNode("onLoad", "perm").setString("default");
                    out.println("Remember that the user will be given default permissions on startup");
                } else {
                    node.getNode("onLoad", "perm").setString("admin");
                    out.println("Remember that the user will be given admin permissions on startup");
                }
            }
            else
                return;
        }

        boolean exitUsrConfigurator = false;
        while (!exitUsrConfigurator) {
            try {
                out.println(CONSDIV);
                out.println("Editing user:" + groupName + " -> " +name);
                out.println("[0] Exit");
                out.println("[1] Change username");
                out.println("[2] Change password");
                out.println("[3] Delete user");
                out.println(CONSDIV);

                try {
                    l = reader.readLine();

                    Integer i = Integer.parseInt(l);
                    switch (i) {
                        case 0: exitUsrConfigurator = true; break;
                        case 1:
                            out.println("Enter new name:");
                            l = reader.readLine();
                            if (!l.isEmpty() && !l.equals("\n") && config.getNode("groups", groupName, "users", l).isVirtual()) {
                                node.setKey(l);
                                name = l;
                            } else {
                                out.println("Username invalid or user exists.");
                            }
                            break;
                        case 2:
                            out.println("Please enter the desired authMethod-ID [default]:");
                            String authMethodID = null;
                            l = reader.readLine();
                            if (l.equals("method")) {
                                out.println("Invalid AuthMethod-ID");
                                continue;
                            } else if (l.isEmpty() || l.equals("\n")) {
                                authMethodID = "default";
                            } else {
                                authMethodID = l;
                            }

                            out.println("Please enter the desired password:");
                            l = reader.readLine();
                            if (l.isEmpty() || l.equals("\n")) {
                                out.println("Invalid AuthMethod-ID");
                                continue;
                            }
                            String password = l;

                            out.println("Registering password change... Will be performed during initialization :)");
                            node.getNode("onLoad", "passwd").getNode("method").setString(authMethodID);
                            node.getNode("onLoad", "passwd").getNode("password").setString(password);
                            break;
                        case 3:
                            out.println("Really delete user: " + name);
                            if (askForConfirm(reader)) {
                                config.getNode("groups", groupName).delNode(name);
                                out.println("Deleted.");
                                return;
                            }
                        default:
                            out.println("Invalid number: Use 0-3");
                            Thread.sleep(1000);
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Unable to read input", e);
                } catch (NumberFormatException e) {
                    out.println("Invalid number, try again");
                    Thread.sleep(1000);
                } catch (RootMustStayHubException e) {
                    //Shouldn't happen for users
                }
            } catch (InterruptedException e) {
                out.println("Interrupted!");
            }
        }
    }

    private static void editConfig(ConfigNode config, BufferedReader reader) throws IOException {
        PrintStream out = System.out;
        out.println("WARNING: THIS IS AN ADVANCED MODE. READ THE REFERENCE BEFORE USING THIS!");

        while (true) {
            out.println("Enter DataQuery. Path divider is '/' ");
            out.println("'exit' for exit ;)");
            String l = reader.readLine();
            if (l.equals("\n") || l.isEmpty()) {
                out.println("Invalid data query");
                continue;
            }
            if (l.equals("exit")) break;

            ConfigNode node = config.getNode(l, '/');

            out.println("Enter new value for: \"" + l + "\"");
            out.println("Supported casts: \"(INT)->\", \"(BOOL)->\"");
            l = reader.readLine();

            if (l.contains("->")) {
                String type = l.substring(0, l.indexOf("->"));
                String value = l.substring(l.indexOf("->") + 2);
                try {
                    switch (type) {
                        case "(INT)":
                            int i = Integer.parseInt(value);
                            node.setInt(i);
                            out.println("Value has been changed ! :)");
                            break;
                        case "(BOOL)":
                            Boolean b = null;
                            if (value.equals("false"))
                                b = Boolean.FALSE;
                            else if (value.equals("true"))
                                b = Boolean.TRUE;
                            else
                                throw new ClassCastException("Cannot cast \"" + value + "\" to BOOLEAN!");

                            node.setBoolean(b);
                            out.println("Value has been changed ! :)");
                    }
                } catch (NumberFormatException | ClassCastException e) {
                    out.println("Unable to change this value:");
                    out.println(e.getMessage());
                    out.println("Just in case: Should we literally add this as string? [y/n]");
                    if (askForConfirm(reader)) {
                        node.setString(l);
                        out.println("Okey, changed the value.");
                    }
                }
            } else {
                node.setString(l);
            }
        }
    }
}
