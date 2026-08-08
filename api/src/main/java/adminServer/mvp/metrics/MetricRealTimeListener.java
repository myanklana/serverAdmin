package adminServer.mvp.metrics;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MetricRealTimeListener {

    private final SimpMessagingTemplate messagingTemplate;

    public MetricRealTimeListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(MetricRealTimeEvent event) {
        MetricRealTimeResponse response = MetricRealTimeResponse.from(event);
        messagingTemplate.convertAndSendToUser(event.ownerId().toString(), "/queue/metrics", response);
    }
}
