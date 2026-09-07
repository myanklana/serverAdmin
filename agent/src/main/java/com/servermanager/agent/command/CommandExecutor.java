package com.servermanager.agent.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servermanager.agent.process.ProcessCollector;
import com.servermanager.agent.process.ProcessTerminator;
import oshi.SystemInfo;
import com.servermanager.agent.AgentApplication.Config;

public final class CommandExecutor {
    private final ObjectMapper json;
    private final ProcessCollector processes;
    private final ProcessTerminator terminator = new ProcessTerminator();
    private final SystemOperations systemOperations;
    public CommandExecutor(ObjectMapper json, SystemInfo system, Config config) { this.json = json; this.processes = new ProcessCollector(system); this.systemOperations = new SystemOperations(json, config.isTerminalEnabled(), config.allowedCommands(), config.allowedPaths()); }
    public CommandResult execute(AgentCommandResponse command) {
        if (command.expiresAt() != null && command.expiresAt().isBefore(java.time.Instant.now())) return CommandResult.failure("Comando expirado");
        try {
            return switch (command.type()) {
                case LIST_PROCESSES -> CommandResult.success(json.writeValueAsString(processes.collect()));
                case TERMINATE_PROCESS -> {
                    long pid = command.payload().path("pid").asLong(-1); terminator.terminate(pid);
                    yield CommandResult.success("{\"pid\":" + pid + ",\"terminated\":true}");
                }
                default -> CommandResult.success(systemOperations.execute(command.type(), command.payload()));
            };
        } catch (Exception exception) { return CommandResult.failure(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()); }
    }
}
