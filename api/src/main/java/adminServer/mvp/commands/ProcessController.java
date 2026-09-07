package adminServer.mvp.commands;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import adminServer.mvp.security.AuthenticatedUser;
import adminServer.mvp.user.User;
import adminServer.mvp.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/servers/{serverId}/processes")
public class ProcessController {
    private final AgentCommandService commands;
    private final UserRepository users;
    public ProcessController(AgentCommandService commands, UserRepository users) { this.commands = commands; this.users = users; }

    @PostMapping("/refresh") @ResponseStatus(HttpStatus.ACCEPTED)
    public AgentCommandService.CommandView refresh(@PathVariable UUID serverId, Authentication auth) {
        return commands.request(serverId, user(auth), AgentCommandType.LIST_PROCESSES, null);
    }
    @PostMapping("/terminate") @ResponseStatus(HttpStatus.ACCEPTED)
    public AgentCommandService.CommandView terminate(@PathVariable UUID serverId, @Valid @RequestBody TerminateRequest request, Authentication auth) {
        return commands.request(serverId, user(auth), AgentCommandType.TERMINATE_PROCESS, request);
    }
    @GetMapping("/commands")
    public List<AgentCommandService.CommandView> history(@PathVariable UUID serverId, Authentication auth) {
        return commands.list(serverId, user(auth).getId());
    }
    @GetMapping("/commands/{id}")
    public AgentCommandService.CommandView command(@PathVariable UUID serverId, @PathVariable UUID id, Authentication auth) {
        return commands.get(id, serverId, user(auth).getId());
    }
    private User user(Authentication auth) { return users.findById(((AuthenticatedUser) auth.getPrincipal()).id()).orElseThrow(); }
    public record TerminateRequest(@Positive long pid) { }
}
