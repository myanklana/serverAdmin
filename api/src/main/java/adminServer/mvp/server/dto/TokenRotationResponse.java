package adminServer.mvp.server.dto;

import java.util.UUID;

public record TokenRotationResponse(UUID serverId, String agentToken) {
}
