package adminServer.mvp.commands;

import java.time.Instant;
import java.util.UUID;

import adminServer.mvp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "agent_commands")
@Getter
public class AgentCommand {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    private AgentCommandType type;

    @Column
    private String payload;

    @Enumerated(EnumType.STRING)
    private AgentCommandStatus status;

    private Instant createdAT;
    private Instant claimedAt;
    private Instant completedAt;

    @Column
    private String result;

    @Column
    private String errorMessage;

}
