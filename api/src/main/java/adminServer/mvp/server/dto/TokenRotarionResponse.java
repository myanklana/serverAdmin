package adminServer.mvp.server.dto;

import java.util.UUID;

public record TokenRotarionResponse(UUID serverId, String token) {

}
