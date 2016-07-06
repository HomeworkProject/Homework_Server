package de.mlessmann.network.commands;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 27.06.16.
 */
public class CommHandProvider {

    private static ArrayList<ICommandHandler> commands = new ArrayList<ICommandHandler>();
    private Logger myLogger = Logger.getGlobal();

    public void registerCommand(ICommandHandler handler) {

        myLogger.finest("Registering command " + handler.getIdentifier());

        if (getByIdentifier(handler.getIdentifier()).size() == 0) {

            commands.add(handler);
            myLogger.finer("Registered command " + handler.getIdentifier());

        } else {

            myLogger.warning("Command " + handler.getIdentifier() + " already registered!");

        }
    }

    public ArrayList<ICommandHandler> getByCommand(String command) {

        ArrayList<ICommandHandler> res = new ArrayList<ICommandHandler>();

        commands.stream().filter(h -> h.getCommand().equals(command)).forEach(res::add);

        return res;

    }

    public ArrayList<ICommandHandler> getByIdentifier(String id) {

        ArrayList<ICommandHandler> res = new ArrayList<ICommandHandler>();

        commands.stream().filter(h -> h.getIdentifier().equals(id)).forEach(res::add);

        return res;

    }

    public ArrayList<ICommandHandler> getHandler() {

        return commands;

    }

    public ArrayList<String> getAllIdentifier() {

        ArrayList<ICommandHandler> h = getHandler();

        ArrayList<String> res = new ArrayList<String>();

        h.forEach(ch -> res.add(ch.getIdentifier()));

        return res;

    }

    public void setLogger(Logger l) { myLogger = l; }

}
