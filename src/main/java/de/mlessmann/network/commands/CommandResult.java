package de.mlessmann.network.commands;

/**
 * Created by Life4YourGames on 10.11.16.
 */
public class CommandResult {

    public enum QuickType {
        UNHANDLED,
        CLIENTFAIL,
        SERVERFAIL,
        SUCCESS
    }

    public static CommandResult unhandled() { return ofQuick(QuickType.UNHANDLED); }

    public static CommandResult clientFail() { return ofQuick(QuickType.CLIENTFAIL); }

    public static CommandResult serverFail() { return ofQuick(QuickType.SERVERFAIL); }

    public static CommandResult success() { return ofQuick(QuickType.SUCCESS); }

    public static CommandResult ofQuick(QuickType type) {
        return new CommandResult().setQuickType(type);
    }

    private QuickType quickType;

    public CommandResult() {
        super();
    }

    public CommandResult setQuickType(QuickType quickType) {
        this.quickType = quickType;
        return this;
    }

    public QuickType getQuickType() { return quickType; }
}
