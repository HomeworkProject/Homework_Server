package de.homeworkproject.server.network.commands;

import de.homeworkproject.server.network.HWClientCommandContext;

import java.util.Optional;

/**
 * Created by Life4YourGames on 27.06.16.
 */
public interface ICommandHandler {

    String getCommand();

    String getIdentifier();

    CommandResult onMessage(HWClientCommandContext context);

    Optional<ICommandHandler> clone();

    boolean isCritical();
}
