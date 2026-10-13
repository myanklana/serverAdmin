package adminServer.mvp.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetricRepository extends JpaRepository<Metric, UUID> {
    Optional<Metric> findFirstByServerIdOrderByCollectedAtDesc(UUID serverId);

    @Query("""
            select m from Metric m
            where m.server.id in :serverIds
              and m.collectedAt = (select max(m2.collectedAt) from Metric m2 where m2.server = m.server)
            """)
    List<Metric> findLatestForServers(@Param("serverIds") Collection<UUID> serverIds);

    @Modifying
    @Query("delete from Metric m where m.collectedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @EntityGraph(attributePaths = "server")
    Page<Metric> findByServerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
            UUID serverId, Instant from, Instant to, Pageable pageable);

    @EntityGraph(attributePaths = "server")
    Page<Metric> findByServerOwnerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
            UUID ownerId, Instant from, Instant to, Pageable pageable);

}
