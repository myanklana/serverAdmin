package adminServer.mvp.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import adminServer.mvp.mappers.MetricMapper;
import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.server.ManagedServerRepository;

@Service
public class MetricIngestionService {

    private static final Duration MAX_PAST_AGE = Duration.ofMinutes(10);
    private static final Duration MAX_FUTURE_AGE = Duration.ofMinutes(2);

    private final ManagedServerRepository managedServerRepository;
    private final MetricRepository metricRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MetricIngestionService(ManagedServerRepository managedServerRepository, MetricRepository metricRepository,
            ApplicationEventPublisher eventPublisher, Clock clock) {
        this.managedServerRepository = managedServerRepository;
        this.metricRepository = metricRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public MetricRealTimeResponse ingest(ManagedServer server, MetricRequest request) {
        if (server == null) {
            throw new IllegalArgumentException("ManagedServer cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("MetricRequest cannot be null");
        }
        validateRequest(request);
        Instant collectedAt = resolveTimeStamp(request.collectedAt());
        server.markOnline(collectedAt, normalizeHostname(request.hostname()));
        managedServerRepository.save(server);

        Metric metric = MetricMapper.from(server, request, collectedAt);
        metric = metricRepository.save(metric);
        MetricRealTimeEvent event = MetricRealTimeEvent.from(metric);
        eventPublisher.publishEvent(event);
        return MetricRealTimeResponse.from(event);
    }

    private void validateRequest(MetricRequest request) {
        if (request.cpuPercent() < 0.0 || request.cpuPercent() > 100.0) {
            throw new IllegalArgumentException("CPU percent must be between 0.0 and 100.0");
        }
        if (request.memoryUsedBytes() < 0) {
            throw new IllegalArgumentException("Memory used bytes must be non-negative");
        }
        if (request.memoryTotalBytes() <= 0) {
            throw new IllegalArgumentException("Memory total bytes must be positive");
        }
        if (request.memoryUsedBytes() > request.memoryTotalBytes()) {
            throw new IllegalArgumentException("Memory used bytes must not exceed memory total bytes");
        }
        if (request.diskUsedBytes() < 0 || request.diskTotalBytes() <= 0
                || request.diskUsedBytes() > request.diskTotalBytes()) {
            throw new IllegalArgumentException("Disk usage is invalid");
        }
    }

    private Instant resolveTimeStamp(Instant requestedTime) {
        Instant now = clock.instant();
        if (requestedTime == null) {
            return now;
        }
        if (requestedTime.isBefore(now.minus(MAX_PAST_AGE))) {
            throw new IllegalArgumentException("Timestamp is too far in the past");
        }
        if (requestedTime.isAfter(now.plus(MAX_FUTURE_AGE))) {
            throw new IllegalArgumentException("Timestamp is too far in the future");
        }
        return requestedTime;

    }

    private String normalizeHostname(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Hostname cannot be null or blank");
        }
        return hostname.trim().toLowerCase();
    }

}
