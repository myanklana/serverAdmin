package adminServer.mvp.metrics;

import java.time.Instant;
import java.util.UUID;

public record MetricRealTimeResponse(
        UUID serverId,
        String serverName,
        Instant collectedAt,
        double cpuPercent,
        long memoryUsedBytes,
        long memoryTotalBytes,
        long diskUsedBytes,
        long diskTotalBytes,
        long uptimeSeconds,
        long networkReceivedBytesPerSecond,
        long networkSentBytesPerSecond) {
    static MetricRealTimeResponse from(
            MetricRealTimeEvent event) {
        return new MetricRealTimeResponse(
                event.serverId(),
                event.serverName(),
                event.collectedAt(),
                event.cpuPercent(),
                event.memoryUsedBytes(),
                event.memoryTotalBytes(),
                event.diskUsedBytes(),
                event.diskTotalBytes(),
                event.uptimeSeconds(),
                event.networkReceivedBytesPerSecond(),
                event.networkSentBytesPerSecond());
    }
}