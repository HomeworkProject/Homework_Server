package de.mlessmann.reflections;

import de.mlessmann.updates.IAppUpdateIndex;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 07.07.16.
 */
public class UpdIndexProvider {

    private ArrayList<IAppUpdateIndex> commands = new ArrayList<IAppUpdateIndex>();
    private Logger myLogger = Logger.getGlobal();

    public void registerIndex(IAppUpdateIndex handler) {

        myLogger.finest("Registering UpdateIndex " + handler.getIdentifier());

        if (getByIdentifier(handler.getIdentifier()).size() == 0) {

            commands.add(handler);
            myLogger.finer("Registered UpdateIndex " + handler.getIdentifier());

        } else {

            myLogger.warning("UpdateIndex " + handler.getIdentifier() + " already registered!");

        }
    }

    public ArrayList<IAppUpdateIndex> getByType(String command) {

        ArrayList<IAppUpdateIndex> res = new ArrayList<IAppUpdateIndex>();

        commands.stream().filter(h -> h.getType().equals(command)).forEach(res::add);

        return res;

    }

    public ArrayList<IAppUpdateIndex> getByIdentifier(String id) {

        ArrayList<IAppUpdateIndex> res = new ArrayList<IAppUpdateIndex>();

        commands.stream().filter(h -> h.getIdentifier().equals(id)).forEach(res::add);

        return res;

    }

    public ArrayList<IAppUpdateIndex> getIndicies() {

        return commands;

    }

    public ArrayList<String> getAllIdentifier() {

        ArrayList<IAppUpdateIndex> h = getIndicies();

        ArrayList<String> res = new ArrayList<String>();

        h.forEach(ch -> res.add(ch.getIdentifier()));

        return res;

    }

    public void setLogger(Logger l) { myLogger = l; }


}
