package adminServer.mvp.server;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import adminServer.mvp.security.AuthenticatedUser;
import adminServer.mvp.metrics.Metric;
import adminServer.mvp.metrics.MetricRepository;

@RestController
@RequestMapping("/api/servers")
public class ServerController {
    private final ManagedServerRepository servers;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MetricRepository metrics;
    public ServerController(ManagedServerRepository servers, UserRepository users, PasswordEncoder passwordEncoder, MetricRepository metrics) {
        this.servers = servers; this.users = users; this.passwordEncoder = passwordEncoder; this.metrics = metrics;
    }
    @GetMapping
    public List<ServerResponse> list(Authentication authentication) {
        User user = currentUser(authentication);
        return servers.findAllByOwnerIdOrderByNameAsc(user.getId()).stream().map(this::response).toList();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServerResponse create(@Valid @RequestBody CreateServerRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        String ip = request.ip().trim(); validateIp(ip);
        String token = request.token().trim();
        ManagedServer server = servers.save(new ManagedServer(request.name().trim(), request.hostnameOrIp(), ip,
                request.port(), passwordEncoder.encode(token), tokenLookupHash(token), user));
        return response(server);
    }
    @GetMapping("/{id}")
    public ServerResponse get(@PathVariable UUID id, Authentication authentication) {
        return response(findOwned(id, authentication));
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) { servers.delete(findOwned(id, authentication)); }

    private ManagedServer findOwned(UUID id, Authentication auth) {
        return servers.findByIdAndOwnerId(id, currentUser(auth).getId()).orElseThrow(java.util.NoSuchElementException::new);
    }
    private User currentUser(Authentication authentication) {
        return users.findById(((AuthenticatedUser) authentication.getPrincipal()).id()).orElseThrow(java.util.NoSuchElementException::new);
    }
    public record CreateServerRequest(@NotBlank @Size(max = 100) String name,
            @Size(max = 255) String hostname,
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F:.]+$", message = "deve ser um endereço IPv4 ou IPv6") String ip,
            @Min(1) @Max(65535) int port,
            @NotBlank @Size(min = 32, max = 512) String token) {
        String hostnameOrIp() { return hostname == null || hostname.isBlank() ? ip.trim() : hostname.trim(); }
    }
    private void validateIp(String ip) { try { InetAddress.getByName(ip); } catch (Exception ex) { throw new IllegalArgumentException("IP inválido"); } }
    private String tokenLookupHash(String token) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private ServerResponse response(ManagedServer server) {
        Metric metric = metrics.findFirstByServerIdOrderByCollectedAtDesc(server.getId()).orElse(null);
        ServerStatus status = server.getLastSeen() != null && server.getLastSeen().isBefore(Instant.now().minusSeconds(15)) ? ServerStatus.OFFLINE : server.getStatus();
        return new ServerResponse(server.getId(), server.getName(), server.getHostname(), server.getIp(), server.getPort(), status, server.getLastSeen(), metric == null ? null : MetricResponse.from(metric));
    }
    public record ServerResponse(UUID id, String name, String hostname, String ip, int port, ServerStatus status, Instant lastSeen, MetricResponse latestMetrics) { }
    public record MetricResponse(Instant collectedAt, double cpuPercent, long memoryUsedBytes, long memoryTotalBytes, long diskUsedBytes, long diskTotalBytes, String operatingSystem, String kernel, String architecture, long uptimeSeconds, long networkReceivedBytes, long networkSentBytes, long networkReceivedBytesPerSecond, long networkSentBytesPerSecond) {
        static MetricResponse from(Metric metric) { return new MetricResponse(metric.getCollectedAt(), metric.getCpuPercent(), metric.getMemoryUsedBytes(), metric.getMemoryTotalBytes(), metric.getDiskUsedBytes(), metric.getDiskTotalBytes(), metric.getOperatingSystem(), metric.getKernel(), metric.getArchitecture(), metric.getUptimeSeconds(), metric.getNetworkReceivedBytes(), metric.getNetworkSentBytes(), metric.getNetworkReceivedBytesPerSecond(), metric.getNetworkSentBytesPerSecond()); }
    }
}
