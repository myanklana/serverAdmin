package adminServer.mvp.metrics;

import adminServer.mvp.server.AgentAuthenticationService;
import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.server.ManagedServerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class MetricsController {
        private final AgentAuthenticationService agentAuthentication;
        private final MetricRepository metrics;
        private final ManagedServerRepository servers;
        private final MetricIngestionService ingestionService;

        public MetricsController(AgentAuthenticationService agentAuthentication, MetricRepository metrics,
                        ManagedServerRepository servers, MetricIngestionService ingestionService) {
                this.agentAuthentication = agentAuthentication;
                this.metrics = metrics;
                this.servers = servers;
                this.ingestionService = ingestionService;
        }

        @PostMapping("/metrics")
        @ResponseStatus(HttpStatus.ACCEPTED)
        public void receive(@RequestHeader("X-Agent-Token") String token, @Valid @RequestBody MetricRequest request) {
                ManagedServer server = agentAuthentication.authenticate(token);
                ingestionService.ingest(server, request);
        }
}
