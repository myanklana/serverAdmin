package adminServer.mvp.alerts;
import java.time.Instant; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
import adminServer.mvp.security.AuthenticatedUser; import adminServer.mvp.user.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
@RestController @RequestMapping("/api/alerts") public class AlertController {
 private final AlertService alerts; private final UserRepository users; public AlertController(AlertService a,UserRepository u){alerts=a;users=u;}
 @PostMapping("/rules") @ResponseStatus(HttpStatus.CREATED) public RuleView create(@Valid @RequestBody CreateRule r,Authentication a){return rule(alerts.create(r.serverId(),user(a),r.metric(),r.threshold(),r.cooldownSeconds()));}
 @GetMapping("/rules") public List<RuleView> rules(Authentication a){return alerts.rules(user(a).getId()).stream().map(this::rule).toList();}
 @GetMapping("/events") public List<EventView> events(Authentication a){return alerts.events(user(a).getId()).stream().map(this::event).toList();}
 @PostMapping("/events/{id}/acknowledge") @ResponseStatus(HttpStatus.NO_CONTENT) public void acknowledge(@PathVariable UUID id,Authentication a){alerts.acknowledge(id,user(a).getId());}
 private User user(Authentication a){return users.findById(((AuthenticatedUser)a.getPrincipal()).id()).orElseThrow();}
 private RuleView rule(AlertRule r){return new RuleView(r.getId(),r.getServer().getId(),r.getServer().getName(),r.getMetric(),r.getThreshold(),r.isEnabled(),r.getCooldownSeconds(),r.getLastTriggeredAt());}
 private EventView event(AlertEvent e){return new EventView(e.getId(),e.getServer().getId(),e.getServer().getName(),e.getRule().getMetric(),e.getValue(),e.getThreshold(),e.getCreatedAt(),e.getAcknowledgedAt());}
 public record CreateRule(@NotNull UUID serverId,@NotNull AlertMetric metric,@DecimalMin("0") @DecimalMax("100") double threshold,@Min(60) @Max(86400) int cooldownSeconds){}
 public record RuleView(UUID id,UUID serverId,String serverName,AlertMetric metric,double threshold,boolean enabled,int cooldownSeconds,Instant lastTriggeredAt){}
 public record EventView(UUID id,UUID serverId,String serverName,AlertMetric metric,double value,double threshold,Instant createdAt,Instant acknowledgedAt){}
}
