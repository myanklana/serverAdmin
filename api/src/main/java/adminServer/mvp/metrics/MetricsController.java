package adminServer.mvp.metrics;

import adminServer.mvp.server.AgentAuthenticationService;
import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.server.ManagedServerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/agent")
public class MetricsController {
    private final AgentAuthenticationService agentAuthentication; private final MetricRepository metrics; private final ManagedServerRepository servers;
    public MetricsController(AgentAuthenticationService agentAuthentication, MetricRepository metrics, ManagedServerRepository servers) { this.agentAuthentication=agentAuthentication; this.metrics=metrics; this.servers=servers; }
    @PostMapping("/metrics") @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(@RequestHeader("X-Agent-Token") String token, @Valid @RequestBody MetricRequest request) {
        ManagedServer server = agentAuthentication.authenticate(token);
        Instant collectedAt = request.collectedAt() == null ? Instant.now() : request.collectedAt();
        server.markOnline(collectedAt, request.hostname());
        servers.save(server);
        metrics.save(new Metric(server, collectedAt, request.cpuPercent(), request.memoryUsedBytes(), request.memoryTotalBytes(), request.diskUsedBytes(), request.diskTotalBytes(), request.operatingSystem(), request.kernel(), request.architecture(), request.uptimeSeconds(), request.networkReceivedBytes(), request.networkSentBytes()));
    }
    public record MetricRequest(@NotBlank String hostname, Instant collectedAt, @DecimalMin("0.0") @DecimalMax("100.0") double cpuPercent,
        @PositiveOrZero long memoryUsedBytes, @Positive long memoryTotalBytes, @PositiveOrZero long diskUsedBytes, @Positive long diskTotalBytes,
        @NotBlank String operatingSystem, @NotBlank String kernel, @NotBlank String architecture, @PositiveOrZero long uptimeSeconds,
        @PositiveOrZero long networkReceivedBytes, @PositiveOrZero long networkSentBytes) { }
}
