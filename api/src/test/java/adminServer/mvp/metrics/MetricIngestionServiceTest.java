package adminServer.mvp.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.server.ManagedServerRepository;
import adminServer.mvp.server.ServerStatus;
import adminServer.mvp.user.User;

@ExtendWith(MockitoExtension.class)
class MetricIngestionServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");

    @Mock private ManagedServerRepository serverRepository;
    @Mock private MetricRepository metricRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private User owner;

    private MetricIngestionService service;
    private ManagedServer server;

    @BeforeEach
    void setUp() {
        server = new ManagedServer("Meu PC", "old-host", "127.0.0.1", 8081, "hash", "lookup", owner);
        service = new MetricIngestionService(serverRepository, metricRepository, eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void persistsMetricMarksServerOnlineAndPublishesEvent() {
        when(owner.getId()).thenReturn(UUID.randomUUID());
        when(metricRepository.save(any(Metric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MetricRealTimeResponse response = service.ingest(server, validRequest("  MY-PC  ", null));

        assertThat(server.getStatus()).isEqualTo(ServerStatus.ONLINE);
        assertThat(server.getHostname()).isEqualTo("my-pc");
        assertThat(server.getLastSeen()).isEqualTo(NOW);
        assertThat(response.collectedAt()).isEqualTo(NOW);
        assertThat(response.serverName()).isEqualTo("Meu PC");
        verify(serverRepository).save(server);

        var metricCaptor = ArgumentCaptor.forClass(Metric.class);
        verify(metricRepository).save(metricCaptor.capture());
        assertThat(metricCaptor.getValue().getCollectedAt()).isEqualTo(NOW);
        assertThat(metricCaptor.getValue().getKernel()).isEqualTo("6.1");

        var eventCaptor = ArgumentCaptor.forClass(MetricRealTimeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().ownerId()).isEqualTo(owner.getId());
        assertThat(eventCaptor.getValue().networkReceivedBytesPerSecond()).isEqualTo(100);
    }

    @Test
    void preservesAcceptedAgentTimestamp() {
        when(owner.getId()).thenReturn(UUID.randomUUID());
        Instant collectedAt = NOW.minusSeconds(60);
        when(metricRepository.save(any(Metric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MetricRealTimeResponse response = service.ingest(server, validRequest("my-pc", collectedAt));

        assertThat(response.collectedAt()).isEqualTo(collectedAt);
        assertThat(server.getLastSeen()).isEqualTo(collectedAt);
    }

    @Test
    void rejectsTimestampOutsideAllowedWindowWithoutPersisting() {
        assertThatThrownBy(() -> service.ingest(server, validRequest("my-pc", NOW.minusSeconds(601))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");

        verify(serverRepository, never()).save(any());
        verify(metricRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsInconsistentMemoryWithoutPersisting() {
        MetricRequest invalid = new MetricRequest("my-pc", NOW, 10, 2_000, 1_000,
                500, 1_000, "Windows", "6.1", "amd64", 100, 10_000, 5_000, 100, 50);

        assertThatThrownBy(() -> service.ingest(server, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Memory used");

        verify(metricRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsBlankHostname() {
        assertThatThrownBy(() -> service.ingest(server, validRequest("  ", NOW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hostname");
    }

    private MetricRequest validRequest(String hostname, Instant collectedAt) {
        return new MetricRequest(hostname, collectedAt, 10, 500, 1_000,
                500, 1_000, "Windows", " 6.1 ", "amd64", 100,
                10_000, 5_000, 100, 50);
    }
}
