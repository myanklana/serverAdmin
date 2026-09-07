package com.servermanager.agent.command;
public record CommandResult(boolean success, String result, String errorMessage) {
    public static CommandResult success(String result) { return new CommandResult(true, result, null); }
    public static CommandResult failure(String message) { return new CommandResult(false, null, message); }
}
