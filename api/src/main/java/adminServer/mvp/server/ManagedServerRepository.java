package adminServer.mvp.server;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagedServerRepository extends JpaRepository<ManagedServer, UUID> {
    List<ManagedServer> findAllByOwnerIdOrderByNameAsc(UUID ownerId);
    Optional<ManagedServer> findByIdAndOwnerId(UUID id, UUID ownerId);
    Optional<ManagedServer> findByTokenLookupHash(String tokenLookupHash);
}
