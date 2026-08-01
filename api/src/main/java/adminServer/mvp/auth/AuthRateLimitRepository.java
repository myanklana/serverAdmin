package adminServer.mvp.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRateLimitRepository extends JpaRepository<AuthRateLimit, UUID> {
    Optional<AuthRateLimit> findByScopeAndIpHash(String scope, String ipHash);
}
