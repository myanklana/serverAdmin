package adminServer.mvp.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MetricRepository extends JpaRepository<Metric, UUID> {
    Optional<Metric> findFirstByServerIdOrderByCollectedAtDesc(UUID serverId);

    @EntityGraph(attributePaths = "server")
    Page<Metric> findByServerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
            UUID serverId, Instant from, Instant to, Pageable pageable);

    @EntityGraph(attributePaths = "server")
    @Query("select metric from Metric metric join CompanyMember member on member.company=metric.server.company where member.user.id=:ownerId and metric.collectedAt>=:from and metric.collectedAt<:to")
    Page<Metric> findByServerOwnerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
            UUID ownerId, Instant from, Instant to, Pageable pageable);

}
