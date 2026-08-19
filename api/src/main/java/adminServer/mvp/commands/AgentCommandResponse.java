package adminServer.mvp.commands;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AgentCommandResponse
 */
public record AgentCommandResponse(
        UUID id,
        AgentCommandType type,
        JsonNode payload,
        Instant createdAt,
        Instant expiresAt) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AgentCommandResponse(UUID id, AgentCommandType type, String payload, Instant createdAt, Instant expiresAt) {
        this(id, type, parsePayload(payload), createdAt, expiresAt);
    }

    private static JsonNode parsePayload(String payload) {
        if (payload == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JSON payload", exception);
        }
    }

}
