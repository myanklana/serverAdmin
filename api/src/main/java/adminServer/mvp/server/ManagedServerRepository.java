package adminServer.mvp.server;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface ManagedServerRepository extends JpaRepository<ManagedServer, UUID> {
    @Query("select s from ManagedServer s join CompanyMember m on m.company=s.company where m.user.id=:ownerId order by s.name")
    List<ManagedServer> findAllByOwnerIdOrderByNameAsc(UUID ownerId);

    @Query("select s from ManagedServer s join CompanyMember m on m.company=s.company where s.id=:id and m.user.id=:ownerId")
    Optional<ManagedServer> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<ManagedServer> findByTokenLookupHash(String tokenLookupHash);

}
