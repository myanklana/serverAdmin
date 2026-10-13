package adminServer.mvp.metrics;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Removes metrics older than the configured retention so the table cannot grow without bound. */
@Component
public class MetricRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(MetricRetentionJob.class);

    private final MetricRepository metrics;
    private final Clock clock;
    private final int retentionDays;

    public MetricRetentionJob(MetricRepository metrics, Clock clock,
            @Value("${app.metrics.retention-days:30}") int retentionDays) {
        this.metrics = metrics;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
    @Transactional
    public void purge() {
        if (retentionDays <= 0) {
            return;
        }
        int removed = metrics.deleteOlderThan(clock.instant().minus(Duration.ofDays(retentionDays)));
        if (removed > 0) {
            log.info("Retenção de métricas: {} registros anteriores a {} dias removidos", removed, retentionDays);
        }
    }
}
