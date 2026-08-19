package adminServer.mvp.metrics;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MetricRealTimeListenerTest {
    @Mock private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendsMetricOnlyToOwnerUserQueue() {
        UUID ownerId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        var event = new MetricRealTimeEvent(ownerId, serverId, "Meu PC",
                Instant.parse("2026-08-08T12:00:00Z"), 20, 500, 1_000,
                300, 1_000, 120, 10_000, 5_000, 100, 50);

        new MetricRealTimeListener(messagingTemplate).publish(event);

        var responseCaptor = ArgumentCaptor.forClass(MetricRealTimeResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq(ownerId.toString()),
                org.mockito.ArgumentMatchers.eq("/queue/metrics"), responseCaptor.capture());
        assertThat(responseCaptor.getValue().serverId()).isEqualTo(serverId);
        assertThat(responseCaptor.getValue().networkSentBytesPerSecond()).isEqualTo(50);
    }
}
