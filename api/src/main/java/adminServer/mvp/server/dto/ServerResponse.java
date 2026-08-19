package adminServer.mvp.server.dto;

import java.time.Instant;
import java.util.UUID;

import adminServer.mvp.server.ServerController.MetricResponse;
import adminServer.mvp.server.ServerStatus;

public record ServerResponse(
                UUID id,
                String name,
                String hostname,
                String ip,
                int port,
                ServerStatus status,
                Instant lastSeen,
                MetricResponse latestMetrics) {
}