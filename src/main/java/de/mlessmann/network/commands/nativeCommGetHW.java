package de.mlessmann.network.commands;

import de.mlessmann.network.HWClientCommandContext;

/**
 * Created by Life4YourGames on 28.06.16.
 */

@HWCommandHandler
public class nativeCommGetHW extends nativeCommandParent {

    public static final String IDENTIFIER = "de.mlessmann.commands.getHW";
    public static final String COMMAND = "gethw";

    public nativeCommGetHW() {

        setID(IDENTIFIER);
        setCommand(COMMAND);

    }

    @Override
    public boolean onMessage(HWClientCommandContext context) {

        //TODO: Command body
        return true;

    }

}
