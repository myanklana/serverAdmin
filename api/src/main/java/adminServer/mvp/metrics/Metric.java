package adminServer.mvp.metrics;

import adminServer.mvp.server.ManagedServer;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "metrics", indexes = @Index(name = "idx_metrics_server_collected_at", columnList = "server_id,collected_at"))
public class Metric {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "server_id", nullable = false) private ManagedServer server;
    @Column(name = "collected_at", nullable = false) private Instant collectedAt;
    @Column(name = "cpu_percent", nullable = false) private double cpuPercent;
    @Column(name = "memory_used_bytes", nullable = false) private long memoryUsedBytes;
    @Column(name = "memory_total_bytes", nullable = false) private long memoryTotalBytes;
    @Column(name = "disk_used_bytes", nullable = false) private long diskUsedBytes;
    @Column(name = "disk_total_bytes", nullable = false) private long diskTotalBytes;
    @Column(nullable = false, length = 255) private String operatingSystem;
    @Column(nullable = false, length = 255) private String kernel;
    @Column(nullable = false, length = 100) private String architecture;
    @Column(name = "uptime_seconds", nullable = false) private long uptimeSeconds;
    @Column(name = "network_received_bytes", nullable = false) private long networkReceivedBytes;
    @Column(name = "network_sent_bytes", nullable = false) private long networkSentBytes;
    protected Metric() { }
    public Metric(ManagedServer server, Instant collectedAt, double cpuPercent, long memoryUsedBytes, long memoryTotalBytes, long diskUsedBytes, long diskTotalBytes, String operatingSystem, String kernel, String architecture, long uptimeSeconds, long networkReceivedBytes, long networkSentBytes) {
        this.server=server; this.collectedAt=collectedAt; this.cpuPercent=cpuPercent; this.memoryUsedBytes=memoryUsedBytes; this.memoryTotalBytes=memoryTotalBytes; this.diskUsedBytes=diskUsedBytes; this.diskTotalBytes=diskTotalBytes; this.operatingSystem=operatingSystem; this.kernel=kernel; this.architecture=architecture; this.uptimeSeconds=uptimeSeconds; this.networkReceivedBytes=networkReceivedBytes; this.networkSentBytes=networkSentBytes;
    }
    public Instant getCollectedAt(){return collectedAt;} public double getCpuPercent(){return cpuPercent;} public long getMemoryUsedBytes(){return memoryUsedBytes;} public long getMemoryTotalBytes(){return memoryTotalBytes;} public long getDiskUsedBytes(){return diskUsedBytes;} public long getDiskTotalBytes(){return diskTotalBytes;} public String getOperatingSystem(){return operatingSystem;} public String getKernel(){return kernel;} public String getArchitecture(){return architecture;} public long getUptimeSeconds(){return uptimeSeconds;} public long getNetworkReceivedBytes(){return networkReceivedBytes;} public long getNetworkSentBytes(){return networkSentBytes;}
}
