package adminServer.mvp.metrics;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import adminServer.mvp.metrics.MetricHistoryService.MetricHistoryResponse;
import adminServer.mvp.security.AuthenticatedUser;

@RestController
@RequestMapping("/api")
public class MetricHistoryController {
    private final MetricHistoryService metricHistoryService;

    public MetricHistoryController(MetricHistoryService metricHistoryService) {
        this.metricHistoryService = metricHistoryService;
    }

    @GetMapping("/metrics/history")
    public Page<MetricHistoryResponse> historyForUser(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return metricHistoryService.getMetricHistoryForUser(
                currentUserId(authentication), from, to, page, size);
    }

    @GetMapping("/servers/{serverId}/metrics")
    public Page<MetricHistoryResponse> historyForServer(
            Authentication authentication,
            @PathVariable UUID serverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return metricHistoryService.getMetricHistoryForServer(
                currentUserId(authentication), serverId, from, to, page, size);
    }

    private UUID currentUserId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).id();
    }
}
