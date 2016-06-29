package de.mlessmann.network.commands;

import de.mlessmann.network.HWClientCommandContext;

/**
 * Created by Life4YourGames on 28.06.16.
 */

@HWCommandHandler
public class nativeCommAddHW extends nativeCommandParent {

    public static final String IDENTIFIER = "de.lessmann.commands.addHW";
    public static final String COMMAND = "addhw";

    public nativeCommAddHW() {

        setID(IDENTIFIER);
        setCommand(COMMAND);

    }

    @Override
    public boolean onMessage(HWClientCommandContext context) {

        //TODO: Command body
        return true;

    }

}
