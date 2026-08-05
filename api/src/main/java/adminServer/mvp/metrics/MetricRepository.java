package adminServer.mvp.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetricRepository extends JpaRepository<Metric, UUID> {
    Optional<Metric> findFirstByServerIdOrderByCollectedAtDesc(UUID serverId);

    Page<Metric> findByServerIdAndCollectedAtBetweenOrderByCollectedAtAsc(UUID serverId, Instant from, Instant to,
            Pageable pageable);

    Page<Metric> findByServerIdInAndCollectedAtBetween(
            Collection<UUID> serverIds,
            Instant from,
            Instant to,
            Pageable pageable);

}
