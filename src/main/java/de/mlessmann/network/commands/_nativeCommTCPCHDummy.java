package de.mlessmann.network.commands;

import de.mlessmann.network.HWClientCommandContext;

import java.util.Optional;

/**
 * Created by Life4YourGames on 29.06.16.
 */
public class _nativeCommTCPCHDummy implements ICommandHandler {

    public static final String ID = "native";

    public static final String COMMAND = "";

    public String getIdentifier() { return ID; }

    public String getCommand() { return COMMAND; }

    public boolean onMessage(HWClientCommandContext c) {
        return true;
    }

    public boolean isCritical() { return false; }

    public Optional<ICommandHandler> clone() { return Optional.of(new _nativeCommTCPCHDummy()); }

}
