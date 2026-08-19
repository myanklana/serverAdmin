package adminServer.mvp.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.server.ManagedServerRepository;

class MetricHistoryServiceTest {
    private ManagedServerRepository servers;
    private MetricRepository metrics;
    private MetricHistoryService service;

    @BeforeEach
    void setUp() {
        servers = mock(ManagedServerRepository.class);
        metrics = mock(MetricRepository.class);
        service = new MetricHistoryService(servers, metrics);
    }

    @Test
    void returnsGlobalPageForAllServersOwnedByUser() {
        UUID userId = UUID.randomUUID();
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-02T00:00:00Z");
        ManagedServer server = mock(ManagedServer.class);
        Metric metric = mock(Metric.class);

        when(server.getId()).thenReturn(UUID.randomUUID());
        when(server.getName()).thenReturn("api-01");
        when(metric.getServer()).thenReturn(server);
        when(metric.getCollectedAt()).thenReturn(from.plusSeconds(5));
        when(metrics.findByServerOwnerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                eq(userId), eq(from), eq(to), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(metric)));

        var result = service.getMetricHistoryForUser(userId, from, to, 0, 100);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().serverId()).isEqualTo(server.getId());
        verify(metrics).findByServerOwnerIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                eq(userId), eq(from), eq(to), org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 100
                                && pageable.getSort().getOrderFor("collectedAt").isAscending()));
    }

    @Test
    void rejectsIntervalsLongerThanThirtyOneDays() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> service.getMetricHistoryForUser(
                UUID.randomUUID(), from, from.plusSeconds(32L * 24 * 60 * 60), 0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("31 days");
    }

    @Test
    void hidesServerNotOwnedByUserAsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-02T00:00:00Z");
        when(servers.findByIdAndOwnerId(serverId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMetricHistoryForServer(
                userId, serverId, from, to, 0, 100))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void validatesPageSize() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-02T00:00:00Z");

        assertThatThrownBy(() -> service.getMetricHistoryForUser(
                UUID.randomUUID(), from, to, 0, 501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
    }
}
