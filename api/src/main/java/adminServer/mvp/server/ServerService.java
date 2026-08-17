package adminServer.mvp.server;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import adminServer.mvp.metrics.Metric;
import adminServer.mvp.metrics.MetricRepository;
import adminServer.mvp.server.ServerController.CreateServerRequest;
import adminServer.mvp.server.ServerController.MetricResponse;
import adminServer.mvp.server.dto.ServerResponse;
import adminServer.mvp.server.dto.TokenRotarionResponse;
import adminServer.mvp.user.User;
import jakarta.transaction.Transactional;

@Service
public class ServerService {

    @Autowired
    TokenService tokenService;

    @Autowired
    ManagedServerRepository serverRepository;

    @Autowired
    MetricRepository metricRepository;

    @Transactional
    public CreatedServer criarServidor(CreateServerRequest req, User user) {

        String rawToken = tokenService.generateToken();

        String passwordHash = tokenService.hashParaVerificar(rawToken);
        String keyHash = tokenService.keyHash(rawToken);

        ManagedServer server = new ManagedServer(
                req.name(),
                req.hostname(),
                req.ip(),
                req.port(),
                passwordHash,
                keyHash,
                user);

        serverRepository.save(server);

        return new CreatedServer(toResponse(server), rawToken);

    }

    @Transactional
    public TokenRotarionResponse rotateToken(UUID serverId, UUID ownerId) {

        ManagedServer server = serverRepository.findByIdAndOwnerId(serverId, ownerId)
                .orElseThrow(NoSuchElementException::new);

        String rawToken = tokenService.generateToken();

        server.setTokenHash(tokenService.hashParaVerificar(rawToken));
        server.setTokenLookupHash(tokenService.keyHash(rawToken));

        return new TokenRotarionResponse(server.getId(), rawToken);

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

    };
}
