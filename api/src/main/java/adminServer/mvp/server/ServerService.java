package adminServer.mvp.server;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import adminServer.mvp.metrics.Metric;
import adminServer.mvp.metrics.MetricRepository;
import adminServer.mvp.server.ServerController.CreateServerRequest;
import adminServer.mvp.server.ServerController.MetricResponse;
import adminServer.mvp.server.dto.ServerResponse;
import adminServer.mvp.server.dto.TokenRotationResponse;
import adminServer.mvp.user.User;
import jakarta.transaction.Transactional;

@Service
public class ServerService {

    private final TokenService tokenService;
    private final ManagedServerRepository serverRepository;
    private final MetricRepository metricRepository;

    public ServerService(TokenService tokenService, ManagedServerRepository serverRepository,
            MetricRepository metricRepository) {
        this.tokenService = tokenService;
        this.serverRepository = serverRepository;
        this.metricRepository = metricRepository;
    }

    @Transactional
    public CreatedServer criarServidor(CreateServerRequest req, User user) {

        String rawToken = req.token() == null || req.token().isBlank()
                ? tokenService.generateToken()
                : req.token().trim();

        String passwordHash = tokenService.hashParaVerificar(rawToken);
        String keyHash = tokenService.keyHash(rawToken);

        ManagedServer server = new ManagedServer(
                req.name().trim(),
                req.hostnameOrIp(),
                req.ip().trim(),
                req.port(),
                passwordHash,
                keyHash,
                user);

        serverRepository.save(server);

        return new CreatedServer(toResponse(server), rawToken);

    }

    @Transactional
    public TokenRotationResponse rotateToken(UUID serverId, UUID ownerId) {

        ManagedServer server = serverRepository.findByIdAndOwnerId(serverId, ownerId)
                .orElseThrow(NoSuchElementException::new);

        String rawToken = tokenService.generateToken();

        server.replaceToken(tokenService.hashParaVerificar(rawToken), tokenService.keyHash(rawToken));
        serverRepository.save(server);

        return new TokenRotationResponse(server.getId(), rawToken);

    }

    private ServerResponse toResponse(ManagedServer server) {
        Metric latest = metricRepository
                .findFirstByServerIdOrderByCollectedAtDesc(server.getId())
                .orElse(null);

        ServerStatus status = calculateStatus(server);

        return new ServerResponse(
                server.getId(),
                server.getName(),
                server.getHostname(),
                server.getIp(),
                server.getPort(),
                status,
                server.getLastSeen(), latest == null ? null : MetricResponse.from(latest));

    }

    private ServerStatus calculateStatus(ManagedServer server) {

        return server.getStatus();

    }

    public record CreatedServer(ServerResponse server, String agentToken) {

    }
}
