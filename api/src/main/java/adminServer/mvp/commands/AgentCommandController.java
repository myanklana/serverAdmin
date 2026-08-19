package adminServer.mvp.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import adminServer.mvp.server.AgentAuthenticationService;
import adminServer.mvp.server.ManagedServer;

@RestController
@RequestMapping("/api/agent/commands")
public class AgentCommandController {

    @Autowired
    private AgentCommandService agentCommandService;

    @Autowired
    private AgentAuthenticationService agentAuthenticationService;

    @PostMapping("/claim")
    public ResponseEntity<AgentCommandResponse> claim(
            @RequestHeader("X-Agent-Token") String token) {
        ManagedServer server = agentAuthenticationService.authenticate(token);

        return agentCommandService.claimNext(server.getId()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
