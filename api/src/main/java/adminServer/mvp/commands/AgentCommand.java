package adminServer.mvp.commands;

import java.time.Instant;
import java.util.UUID;
import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "agent_commands")
public class AgentCommand {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "server_id", nullable = false)
    private ManagedServer server;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    @Enumerated(EnumType.STRING) @Column(name = "command_type", nullable = false, length = 40)
    private AgentCommandType type;
    @Column(columnDefinition = "text") private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AgentCommandStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "claimed_at") private Instant claimedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(columnDefinition = "text") private String result;
    @Column(name = "error_message", columnDefinition = "text") private String errorMessage;

    protected AgentCommand() { }
    public AgentCommand(ManagedServer server, User requestedBy, AgentCommandType type, String payload) {
        this.id = UUID.randomUUID(); this.server = server; this.requestedBy = requestedBy;
        this.type = type; this.payload = payload; this.status = AgentCommandStatus.PENDING; this.createdAt = Instant.now();
    }
    public void claim(Instant at) { status = AgentCommandStatus.CLAIMED; claimedAt = at; }
    public void complete(boolean succeeded, String result, String error, Instant at) {
        status = succeeded ? AgentCommandStatus.SUCCEEDED : AgentCommandStatus.FAILED;
        this.result = result; errorMessage = error; completedAt = at;
    }
    public UUID getId() { return id; }
    public ManagedServer getServer() { return server; }
    public AgentCommandType getType() { return type; }
    public String getPayload() { return payload; }
    public AgentCommandStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
}
