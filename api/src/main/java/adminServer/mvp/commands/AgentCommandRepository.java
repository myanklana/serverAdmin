package adminServer.mvp.commands;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface AgentCommandRepository extends JpaRepository<AgentCommand, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AgentCommand c where c.server.id = :serverId and c.status = adminServer.mvp.commands.AgentCommandStatus.PENDING order by c.createdAt")
    List<AgentCommand> findNextPending(UUID serverId, Pageable pageable);
    Optional<AgentCommand> findByIdAndServerId(UUID id, UUID serverId);
    @Query("select c from AgentCommand c join CompanyMember m on m.company=c.server.company where c.id=:id and c.server.id=:serverId and m.user.id=:ownerId")
    Optional<AgentCommand> findAccessible(UUID id, UUID serverId, UUID ownerId);
    @Query("select c from AgentCommand c join CompanyMember m on m.company=c.server.company where c.server.id=:serverId and m.user.id=:ownerId order by c.createdAt desc")
    List<AgentCommand> findAccessibleHistory(UUID serverId, UUID ownerId, Pageable pageable);
}
