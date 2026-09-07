package com.servermanager.agent.command;

/**
 * AgentCommandType
 */
public enum AgentCommandType {
    LIST_PROCESSES,
    TERMINATE_PROCESS,
    LIST_SERVICES,
    CONTROL_SERVICE,
    LIST_CONTAINERS,
    CONTROL_CONTAINER,
    CONTAINER_LOGS,
    SYSTEM_LOGS,
    RUN_TERMINAL_COMMAND,
    WRITE_FILE,
    READ_FILE,
    CHECK_UPDATE

}
