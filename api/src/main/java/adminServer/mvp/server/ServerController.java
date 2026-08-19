package adminServer.mvp.server;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import adminServer.mvp.metrics.Metric;
import adminServer.mvp.metrics.MetricRepository;
import adminServer.mvp.security.AuthenticatedUser;
import adminServer.mvp.server.dto.ServerResponse;
import adminServer.mvp.server.dto.TokenRotationResponse;
import adminServer.mvp.user.User;
import adminServer.mvp.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servers")
public class ServerController {
    private final ManagedServerRepository servers;
    private final UserRepository users;
    private final MetricRepository metrics;
    private final ServerService serverService;

    public ServerController(ManagedServerRepository servers, UserRepository users,
            MetricRepository metrics, ServerService serverService) {
        this.servers = servers;
        this.users = users;
        this.metrics = metrics;
        this.serverService = serverService;
    }

    @GetMapping
    public List<ServerResponse> list(Authentication authentication) {
        User user = currentUser(authentication);
        return servers.findAllByOwnerIdOrderByNameAsc(user.getId()).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedServerResponse create(@Valid @RequestBody CreateServerRequest request,
            Authentication authentication) {
        validateIp(request.ip().trim());
        return toCreatedResponse(serverService.criarServidor(request, currentUser(authentication)));
    }

    @PostMapping("/{id}/rotate-token")
    public TokenRotationResponse rotate(@PathVariable UUID id, Authentication authentication) {
        return serverService.rotateToken(id, currentUser(authentication).getId());
    }

    @GetMapping("/{id}")
    public ServerResponse get(@PathVariable UUID id, Authentication authentication) {
        return toResponse(findOwned(id, authentication));
    }

    @PatchMapping("/{id}")
    public ServerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateServerRequest request,
            Authentication authentication) {
        ManagedServer server = findOwned(id, authentication);
        String ip = request.ip().trim();
        validateIp(ip);
        server.updateDetails(request.name().trim(), request.hostnameOrIp(), ip, request.port());
        return toResponse(servers.save(server));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        servers.delete(findOwned(id, authentication));
    }

    private CreatedServerResponse toCreatedResponse(ServerService.CreatedServer created) {
        return new CreatedServerResponse(created.server(), created.agentToken());
    }

    private ServerResponse toResponse(ManagedServer server) {
        Metric metric = metrics.findFirstByServerIdOrderByCollectedAtDesc(server.getId()).orElse(null);
        ServerStatus status = server.getLastSeen() != null
                && server.getLastSeen().isBefore(Instant.now().minusSeconds(15))
                        ? ServerStatus.OFFLINE
                        : server.getStatus();
        return new ServerResponse(server.getId(), server.getName(), server.getHostname(), server.getIp(),
                server.getPort(), status, server.getLastSeen(), metric == null ? null : MetricResponse.from(metric));
    }

    private ManagedServer findOwned(UUID id, Authentication auth) {
        return servers.findByIdAndOwnerId(id, currentUser(auth).getId())
                .orElseThrow(java.util.NoSuchElementException::new);
    }

    private User currentUser(Authentication authentication) {
        return users.findById(((AuthenticatedUser) authentication.getPrincipal()).id())
                .orElseThrow(java.util.NoSuchElementException::new);
    }

    private void validateIp(String ip) {
        try {
            InetAddress.getByName(ip);
        } catch (Exception ex) {
            throw new IllegalArgumentException("IP inválido");
        }
    }

    public record CreateServerRequest(@NotBlank @Size(max = 100) String name,
            @Size(max = 255) String hostname,
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F:.]+$", message = "deve ser um endereço IPv4 ou IPv6") String ip,
            @Min(1) @Max(65535) int port,
            @Size(min = 32, max = 512) String token) {
        String hostnameOrIp() {
            return hostname == null || hostname.isBlank() ? ip.trim() : hostname.trim();
        }
    }

    public record UpdateServerRequest(@NotBlank @Size(max = 100) String name,
            @Size(max = 255) String hostname,
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F:.]+$", message = "deve ser um endereço IPv4 ou IPv6") String ip,
            @Min(1) @Max(65535) int port) {
        String hostnameOrIp() {
            return hostname == null || hostname.isBlank() ? ip.trim() : hostname.trim();
        }
    }

    public record CreatedServerResponse(ServerResponse server, String agentToken) { }

    public record MetricResponse(Instant collectedAt, double cpuPercent, long memoryUsedBytes, long memoryTotalBytes,
            long diskUsedBytes, long diskTotalBytes, String operatingSystem, String kernel, String architecture,
            long uptimeSeconds, long networkReceivedBytes, long networkSentBytes, long networkReceivedBytesPerSecond,
            long networkSentBytesPerSecond) {
        static MetricResponse from(Metric metric) {
            return new MetricResponse(metric.getCollectedAt(), metric.getCpuPercent(), metric.getMemoryUsedBytes(),
                    metric.getMemoryTotalBytes(), metric.getDiskUsedBytes(), metric.getDiskTotalBytes(),
                    metric.getOperatingSystem(), metric.getKernel(), metric.getArchitecture(), metric.getUptimeSeconds(),
                    metric.getNetworkReceivedBytes(), metric.getNetworkSentBytes(),
                    metric.getNetworkReceivedBytesPerSecond(), metric.getNetworkSentBytesPerSecond());
        }
    }
}
