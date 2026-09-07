package adminServer.mvp.alerts;
import java.time.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.*;
import adminServer.mvp.metrics.Metric; import adminServer.mvp.server.*; import adminServer.mvp.user.User;
@Service public class AlertService {
 private final AlertRuleRepository rules; private final AlertEventRepository events; private final ManagedServerRepository servers;
 public AlertService(AlertRuleRepository r,AlertEventRepository e,ManagedServerRepository s){rules=r;events=e;servers=s;}
 @Transactional public AlertRule create(UUID serverId,User user,AlertMetric metric,double threshold,int cooldown){if(threshold<0||threshold>100)throw new IllegalArgumentException("Limite deve estar entre 0 e 100");return rules.save(new AlertRule(servers.findByIdAndOwnerId(serverId,user.getId()).orElseThrow(),user,metric,threshold,Math.max(60,cooldown)));}
 @Transactional public void evaluate(Metric metric){Instant now=metric.getCollectedAt();for(AlertRule rule:rules.findByServerIdAndEnabledTrue(metric.getServer().getId())){double value=switch(rule.getMetric()){case CPU->metric.getCpuPercent();case MEMORY->percent(metric.getMemoryUsedBytes(),metric.getMemoryTotalBytes());case DISK->percent(metric.getDiskUsedBytes(),metric.getDiskTotalBytes());};if(rule.shouldTrigger(value,now)){events.save(new AlertEvent(rule,value,now));rule.triggered(now);}}}
 @Transactional(readOnly=true) public List<AlertRule> rules(UUID owner){return rules.findByServerOwnerIdOrderByServerNameAsc(owner);}
 @Transactional(readOnly=true) public List<AlertEvent> events(UUID owner){return events.findAccessible(owner,org.springframework.data.domain.PageRequest.of(0,100));}
 @Transactional public void acknowledge(UUID id,UUID owner){events.findAccessible(id,owner).orElseThrow().acknowledge(Instant.now());}
 private double percent(long u,long t){return t==0?0:u*100d/t;}
}
