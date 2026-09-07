package adminServer.mvp.commands;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import adminServer.mvp.security.AuthenticatedUser;
import adminServer.mvp.user.User;
import adminServer.mvp.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/servers/{serverId}/operations")
public class RemoteAdminController {
    private static final Set<AgentCommandType> ALLOWED = Set.of(AgentCommandType.LIST_SERVICES,
            AgentCommandType.CONTROL_SERVICE, AgentCommandType.LIST_CONTAINERS, AgentCommandType.CONTROL_CONTAINER,
            AgentCommandType.CONTAINER_LOGS, AgentCommandType.SYSTEM_LOGS, AgentCommandType.RUN_TERMINAL_COMMAND,
            AgentCommandType.WRITE_FILE, AgentCommandType.READ_FILE, AgentCommandType.CHECK_UPDATE);
    private final AgentCommandService commands;
    private final UserRepository users;
    public RemoteAdminController(AgentCommandService commands, UserRepository users) { this.commands = commands; this.users = users; }

    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public AgentCommandService.CommandView request(@PathVariable UUID serverId, @Valid @RequestBody OperationRequest request,
            Authentication authentication) {
        if (!ALLOWED.contains(request.type())) throw new IllegalArgumentException("Tipo de operação não permitido nesta rota");
        validatePayload(request);
        return commands.request(serverId, user(authentication), request.type(), request.payload());
    }
    @GetMapping("/{commandId}")
    public AgentCommandService.CommandView result(@PathVariable UUID serverId, @PathVariable UUID commandId, Authentication auth) {
        return commands.get(commandId, serverId, user(auth).getId());
    }
    private User user(Authentication auth) { return users.findById(((AuthenticatedUser) auth.getPrincipal()).id()).orElseThrow(); }
    private void validatePayload(OperationRequest request) {
        Map<String, String> p = request.payload() == null ? Map.of() : request.payload();
        switch (request.type()) {
            case CONTROL_SERVICE -> require(p, "name", "[a-zA-Z0-9@_.-]{1,120}");
            case CONTROL_CONTAINER, CONTAINER_LOGS -> require(p, "container", "[a-zA-Z0-9_.-]{1,128}");
            case RUN_TERMINAL_COMMAND -> { String command = p.get("command"); if (command == null || command.isBlank() || command.length() > 1000) throw new IllegalArgumentException("Comando inválido"); }
            case WRITE_FILE -> { requirePath(p); String content = p.get("contentBase64"); if (content == null || content.length() > 1_400_000) throw new IllegalArgumentException("Arquivo ausente ou maior que 1 MiB"); }
            case READ_FILE -> requirePath(p);
            default -> { }
        }
    }
    private void requirePath(Map<String,String> p) { String path = p.get("path"); if (path == null || path.isBlank() || path.length() > 500 || path.indexOf('\0') >= 0) throw new IllegalArgumentException("Caminho inválido"); }
    private void require(Map<String,String> p, String key, String pattern) { if (p.get(key) == null || !p.get(key).matches(pattern)) throw new IllegalArgumentException(key + " inválido"); }
    public record OperationRequest(@NotNull AgentCommandType type, @Size(max = 10) Map<String, String> payload) { }
}
