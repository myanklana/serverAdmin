package adminServer.mvp.metrics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface MetricRepository extends JpaRepository<Metric, UUID> { Optional<Metric> findFirstByServerIdOrderByCollectedAtDesc(UUID serverId); }
