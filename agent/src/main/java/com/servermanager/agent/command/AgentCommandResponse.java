package com.servermanager.agent.command;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * AgentCommandResponse
 */
public record AgentCommandResponse(
        UUID id,
        AgentCommandType type,
        JsonNode payload,
        Instant createdAt,
        Instant expiresAt) {

}
