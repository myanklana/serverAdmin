package adminServer.mvp.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import adminServer.mvp.server.ManagedServerRepository;

@Service
public class MetricHistoryService {
    private static final Duration MAX_HISTORY_PERIOD = Duration.ofDays(31);
    private static final int MAX_PAGE_SIZE = 500;

    private final ManagedServerRepository managedServerRepository;
    private final MetricRepository metricRepository;

    public MetricHistoryService(ManagedServerRepository managedServerRepository, MetricRepository metricRepository) {
        this.managedServerRepository = managedServerRepository;
        this.metricRepository = metricRepository;
    }

    @Transactional(readOnly = true)
    public Page<MetricHistoryResponse> getMetricHistoryForUser(
            UUID userId, Instant from, Instant to, int page, int size) {
        validateArguments(userId, from, to, page, size);

        return metricRepository
                .findByServerOwnerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                        userId, from, to, pageRequest(page, size))
                .map(MetricHistoryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<MetricHistoryResponse> getMetricHistoryForServer(
            UUID userId, UUID serverId, Instant from, Instant to, int page, int size) {
        validateArguments(userId, from, to, page, size);
        if (serverId == null) {
            throw new IllegalArgumentException("Server id cannot be null");
        }

        managedServerRepository.findByIdAndOwnerId(serverId, userId)
                .orElseThrow(NoSuchElementException::new);

        return metricRepository
                .findByServerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                        serverId, from, to, pageRequest(page, size))
                .map(MetricHistoryResponse::from);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "collectedAt"));
    }

    private void validateArguments(UUID userId, Instant from, Instant to, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Time interval cannot be null");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Invalid time interval: 'from' must be before 'to'");
        }
        if (Duration.between(from, to).compareTo(MAX_HISTORY_PERIOD) > 0) {
            throw new IllegalArgumentException("Time interval cannot exceed 31 days");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 500");
        }
    }

    public record MetricHistoryResponse(
            UUID serverId,
            String serverName,
            Instant collectedAt,
            double cpuPercent,
            long memoryUsedBytes,
            long memoryTotalBytes,
            long diskUsedBytes,
            long diskTotalBytes,
            String operatingSystem,
            String kernel,
            String architecture,
            long uptimeSeconds,
            long networkReceivedBytes,
            long networkSentBytes,
            long networkReceivedBytesPerSecond,
            long networkSentBytesPerSecond) {
        static MetricHistoryResponse from(Metric metric) {
            return new MetricHistoryResponse(
                    metric.getServer().getId(),
                    metric.getServer().getName(),
                    metric.getCollectedAt(),
                    metric.getCpuPercent(),
                    metric.getMemoryUsedBytes(),
                    metric.getMemoryTotalBytes(),
                    metric.getDiskUsedBytes(),
                    metric.getDiskTotalBytes(),
                    metric.getOperatingSystem(),
                    metric.getKernel(),
                    metric.getArchitecture(),
                    metric.getUptimeSeconds(),
                    metric.getNetworkReceivedBytes(),
                    metric.getNetworkSentBytes(),
                    metric.getNetworkReceivedBytesPerSecond(),
                    metric.getNetworkSentBytesPerSecond());
        }
    }
}
