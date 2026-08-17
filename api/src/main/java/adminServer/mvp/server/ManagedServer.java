package adminServer.mvp.server;

import adminServer.mvp.user.User;
import jakarta.persistence.*;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Table(name = "servers", uniqueConstraints = @UniqueConstraint(name = "uk_server_owner_name", columnNames = {
        "owner_id", "name" }))
public class ManagedServer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 255)
    private String hostname;
    @Column(nullable = false, length = 45)
    private String ip;
    @Column(nullable = false)
    private int port;
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;
    @Column(name = "token_lookup_hash", nullable = false, unique = true, length = 64)
    private String tokenLookupHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerStatus status = ServerStatus.PENDING;
    @Column(name = "last_seen")
    private Instant lastSeen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    protected ManagedServer() {
    }

    public ManagedServer(String name, String hostname, String ip, int port, String tokenHash, String tokenLookupHash,
            User owner) {
        this.name = name;
        this.hostname = hostname;
        this.ip = ip;
        this.port = port;
        this.tokenHash = tokenHash;
        this.tokenLookupHash = tokenLookupHash;
        this.owner = owner;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public User getOwner() {
        return owner;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void markOnline(Instant seenAt, String reportedHostname) {
        this.status = ServerStatus.ONLINE;
        this.lastSeen = seenAt;
        if (reportedHostname != null && !reportedHostname.isBlank())
            this.hostname = reportedHostname;
    }
}
