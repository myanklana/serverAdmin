package adminServer.mvp.alerts;
import java.time.Instant;
import java.util.UUID;
import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.user.User;
import jakarta.persistence.*;
@Entity @Table(name="alert_rules")
public class AlertRule {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="server_id") private ManagedServer server;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;
 @Enumerated(EnumType.STRING) private AlertMetric metric; private double threshold; private boolean enabled;
 @Column(name="cooldown_seconds") private int cooldownSeconds; @Column(name="last_triggered_at") private Instant lastTriggeredAt;
 protected AlertRule(){} public AlertRule(ManagedServer s,User u,AlertMetric m,double t,int c){id=UUID.randomUUID();server=s;createdBy=u;metric=m;threshold=t;cooldownSeconds=c;enabled=true;}
 public boolean shouldTrigger(double value,Instant now){return enabled&&value>=threshold&&(lastTriggeredAt==null||lastTriggeredAt.plusSeconds(cooldownSeconds).isBefore(now));}
 public void triggered(Instant at){lastTriggeredAt=at;} public UUID getId(){return id;} public ManagedServer getServer(){return server;} public AlertMetric getMetric(){return metric;} public double getThreshold(){return threshold;} public boolean isEnabled(){return enabled;} public int getCooldownSeconds(){return cooldownSeconds;} public Instant getLastTriggeredAt(){return lastTriggeredAt;}
}
