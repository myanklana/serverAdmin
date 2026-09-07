package adminServer.mvp.alerts;
import java.time.Instant; import java.util.UUID; import adminServer.mvp.server.ManagedServer; import jakarta.persistence.*;
@Entity @Table(name="alert_events") public class AlertEvent {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="rule_id") private AlertRule rule; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="server_id") private ManagedServer server;
 @Column(name="metric_value") private double value; private double threshold; @Column(name="created_at") private Instant createdAt; @Column(name="acknowledged_at") private Instant acknowledgedAt;
 protected AlertEvent(){} public AlertEvent(AlertRule r,double v,Instant at){id=UUID.randomUUID();rule=r;server=r.getServer();value=v;threshold=r.getThreshold();createdAt=at;}
 public void acknowledge(Instant at){acknowledgedAt=at;} public UUID getId(){return id;} public AlertRule getRule(){return rule;} public ManagedServer getServer(){return server;} public double getValue(){return value;} public double getThreshold(){return threshold;} public Instant getCreatedAt(){return createdAt;} public Instant getAcknowledgedAt(){return acknowledgedAt;}
}
