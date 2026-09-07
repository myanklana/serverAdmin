package com.servermanager.agent.command;

public final class CommandPoller implements Runnable {
    private final AgentApiClient api;
    private final CommandExecutor executor;
    public CommandPoller(AgentApiClient api, CommandExecutor executor) { this.api = api; this.executor = executor; }
    @Override public void run() {
        try {
            var command = api.claimNext();
            if (command.isPresent()) api.complete(command.get(), executor.execute(command.get()));
        } catch (Exception exception) { System.err.println("Falha no processamento de comandos: " + exception.getMessage()); }
    }
}
