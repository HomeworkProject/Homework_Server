package de.mlessmann.hwserver.main;

import de.mlessmann.hwserver.allocation.GroupMgrSvc;
import de.mlessmann.hwserver.allocation.GroupSvc;

import java.io.*;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

/**
 * Created by Life4YourGames on 02.09.16.
 */
public class CommandLine {

    private PrintStream out;
    private InputStream in;

    private HWServer server;
    private final Logger LOG;
    private boolean exited;
    private BufferedReader reader;

    public CommandLine(HWServer server) {
        this.server = server;
        LOG = server.getLogger();
    }

    public InputStream getIn() {
        return in;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void run() {
        reader = new BufferedReader(new InputStreamReader(in));
        exited = false;
        loop: while (!exited) {

            try {

                String command = reader.readLine();

                switch (command) {
                    case "stop":
                        server.stop();
                        break;
                    case "quit": ;
                    case "exit":
                        exit(false);
                        break;
                    case "reload":
                        server.stop().start();
                        break;
                    case "start":
                        server.start();
                        break;
                    case "update":
                        server.checkForUpdate();
                        break;
                    case "upgrade":
                        server.upgrade();
                        break;

                    default:
                        if (!processCommand(command))
                            out.println("Unknown command: " + command); break;
                }


            } catch (IOException ex) {
                LOG.throwing(this.getClass().toString(), "start", ex);
            }
        }
    }

    public void exit(boolean forced) {
        server.stop();
        exited = true;
        if (forced) {
            System.exit(0);
        }
    }

    public boolean processCommand(String command) {
        String[] c = command.split(" ");

        switch (c[0]) {
            case "group": groupComm(c); break;
            case "user": userComm(c); break;
            default: return false;
        }
        return true;
    }

    private void groupComm(String[] c) {
        if (c.length < 3) {
            out.println("Invalid usage: group <group> <action>");
        }
        Optional<GroupSvc> optG = server.getGroupManager().getGroup(c[1]);
        if (c[2].equals("create")) {
            if (optG.isPresent()) {
                out.println("Group already exists");
                return;
            }
            if (c[1].isEmpty()) {
                out.println("Empty group name not allowed");
                return;
            }
            server.getGroupManager().createGroup(c[1]);
        }

    }

    private void userComm(String[] c) {
    }

    private void create(String command) {
        String[] c = command.split(" ");

        if (c.length < 2) {
            server.onMessage(this, INFO, "Second argument required!");
            return;
        }
        String type = c[1];
        switch (type) {
            case "group": createGroup(c); break;
            case "user": createUser(c); break;
            case "hw": createHW(c); break;
            default: server.onMessage(this, INFO, "Unknown object type: " + type);
        }
    }

    private void createGroup(String[] c) {
        if (c.length < 3) {
            server.onMessage(this, INFO, "Please specify a group name");
            return;
        }
        String name = c[2];
        if (name.isEmpty()) {
            server.onMessage(this, INFO, "Group name may not be empty");
            return;
        }
        GroupMgrSvc svc = server.getGroupManager();
        if (svc.getGroup(name).isPresent()) {
            server.onMessage(this, INFO, "Group already exists");
            return;
        }
        if (!svc.createGroup(name)) {
            server.onMessage(this, WARNING, "Unable to create group \"" + name + '\"');
            return;
        }
        if (!svc.loadGroup(name)) {
            server.onMessage(this, INFO, "Group cannot be loaded...");
            return;
        }
    }

    private void createUser(String[] c) {
        if (c.length < 4) {
            server.onMessage(this, INFO, "Please specify group and username");
            return;
        }
        String g = c[2];
        GroupSvc group = server.getGroupManager().getGroupNullable(g);
        if (group==null) {
            server.onMessage(this, INFO, "Unknown group");
            return;
        }
        if (g.isEmpty()) {
            server.onMessage(this, INFO, "Group name may not be empty");
            return;
        }
        String name = c[3];
        if (name.isEmpty()) {
            server.onMessage(this, INFO, "User name may not be empty");
            return;
        }
        if (group.createUser(name)) {
            server.onMessage(this, INFO, "User created reloading group");
            if (group.init(group.getNode())) {
                server.onMessage(this, INFO, "Group cannot be reloaded...");
            }
        }
    }

    private void createHW(String[] c) {
        if (c.length < 6) {
            server.onMessage(this, INFO, "Usage: create hw <yyyy-MM-dd> <subject> <title> <description>");
            return;
        }
        server.onMessage(this, INFO, "Creating HWs from command line is not yet supported.");
    }

}
