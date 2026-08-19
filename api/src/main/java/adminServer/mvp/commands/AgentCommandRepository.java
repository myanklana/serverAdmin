package adminServer.mvp.commands;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface AgentCommandRepository extends JpaRepository<AgentCommand, UUID> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        select command
                        from AgentCommand command
                        where command.server.id := serverID
                        and command.status = 'PENDING'
                        order by command.createdAt
                        """)
        List<AgentCommand> findNextPending(
                        UUID serverId,
                        Pageable pageable);

        @Query("""
                        update command
                        from AgentCommand command
                        set command.status :=status, command.claimedAt := claimed
                        where command.id := id
                                """)
        void updateCommandStatus(
                        AgentCommandStatus status,
                        Instant claimed,
                        UUID id);

        List<AgentCommand> findByCommandICommands(UUID id);
}
