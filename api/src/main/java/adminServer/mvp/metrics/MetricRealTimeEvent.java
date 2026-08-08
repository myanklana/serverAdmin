package adminServer.mvp.metrics;

import java.time.Instant;
import java.util.UUID;

public record MetricRealTimeEvent(
        UUID ownerId,
        UUID serverId,
        String serverName,
        Instant collectedAt,
        double cpuPercent,
        long memoryUsedBytes,
        long memoryTotalBytes,
        long diskUsedBytes,
        long diskTotalBytes,
        long uptimeSeconds,
        long networkReceivedBytes,
        long networkSentBytes,
        long networkReceivedBytesPerSecond,
        long networkSentBytesPerSecond) {
    public static MetricRealTimeEvent from(Metric metric) {
        return new MetricRealTimeEvent(
                metric.getServer().getOwner().getId(),
                metric.getServer().getId(),
                metric.getServer().getName(),
                metric.getCollectedAt(),
                metric.getCpuPercent(),
                metric.getMemoryUsedBytes(),
                metric.getMemoryTotalBytes(),
                metric.getDiskUsedBytes(),
                metric.getDiskTotalBytes(),
                metric.getUptimeSeconds(),
                metric.getNetworkReceivedBytes(),
                metric.getNetworkSentBytes(),
                metric.getNetworkReceivedBytesPerSecond(),
                metric.getNetworkSentBytesPerSecond());
    }

}
