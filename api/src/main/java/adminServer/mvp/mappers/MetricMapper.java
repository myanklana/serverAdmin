package adminServer.mvp.mappers;

import java.time.Instant;

import adminServer.mvp.metrics.Metric;
import adminServer.mvp.metrics.MetricRequest;
import adminServer.mvp.server.ManagedServer;

public class MetricMapper {

    private MetricMapper() {
    }

    public static Metric from(ManagedServer server, MetricRequest request, Instant collectedAt) {
        return new Metric(server,
                collectedAt, request.cpuPercent(), request.memoryUsedBytes(),
                request.memoryTotalBytes(), request.diskUsedBytes(), request.diskTotalBytes(),
                request.operatingSystem(), request.kernel().trim(), request.architecture(),
                request.uptimeSeconds(),
                request.networkReceivedBytes(), request.networkSentBytes(),
                request.networkReceivedBytesPerSecond(), request.networkSentBytesPerSecond());
    }

}
