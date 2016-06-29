package de.mlessmann.network.commands;

import de.mlessmann.network.HWClientCommandContext;

import java.util.Optional;

/**
 * Created by Life4YourGames on 27.06.16.
 */
public interface ICommandHandler {

    public abstract String getCommand();

    public abstract String getIdentifier();

    public abstract boolean onMessage(HWClientCommandContext context);

    public abstract Optional<ICommandHandler> clone();

    public abstract boolean isCritical();

}
