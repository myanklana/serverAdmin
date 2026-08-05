package adminServer.mvp.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.server.ManagedServerRepository;
import adminServer.mvp.user.User;

@Service
public class MetricHistoryService {

    @Autowired
    ManagedServerRepository managedServerRepository;

    @Autowired
    MetricRepository metricRepository;

    @Transactional(readOnly = true)
    public Page<Metric> getMetricHistoryForUser(User user, Instant from, Instant to) {
        validateArguments(user, from, to);
        List<ManagedServer> servers = verificaServidoresUsuario(user);
        if (servers.isEmpty()) {
            throw new IllegalArgumentException("User has no managed servers");
        }
        return metricRepository.findByServerIdInAndCollectedAtBetween(
                servers.stream().map(ManagedServer::getId).toList(), from, to, PageRequest.of(0, 10));
    }

    @Transactional(readOnly = true)
    public Page<Metric> getMetricHistoryForServer(User user, UUID serverId, Instant from, Instant to) {
        validateArguments(user, from, to);
        ManagedServer server = managedServerRepository.findByIdAndOwnerId(serverId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Server not found or not owned by user"));
        return returnHistoricDate(server.getId(), from, to);
    }

    private List<ManagedServer> verificaServidoresUsuario(User user) {
        return managedServerRepository.findAllByOwnerIdOrderByNameAsc(user.getId());

    }

    private void validateArguments(User user, Instant from, Instant to) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Time interval cannot be null");
        }
        if (!isIntervalCorrect(from, to)) {
            throw new IllegalArgumentException("Invalid time interval: 'from' must be before 'to'");
        }
        if (!validatePeriod(from, to)) {
            throw new IllegalArgumentException("Invalid time interval: 'from' and 'to' must be within 31 days");
        }
        if (!isIntervalCorrect(from, to)) {
            throw new IllegalArgumentException("Invalid time interval: 'from' must be before 'to'");
        }

    }

    private boolean validatePeriod(Instant from, Instant to) {
        return from.plus(Duration.ofDays(31)).isBefore(to);
    }

    private boolean isIntervalCorrect(Instant from, Instant to) {
        return from.isBefore(to);
    }

    private Page<Metric> returnHistoricDate(UUID serverId, Instant from, Instant to) {

        return metricRepository.findByServerIdAndCollectedAtBetweenOrderByCollectedAtAsc(serverId, from, to,
                PageRequest.of(0, 10));
    }

}
