package adminServer.mvp.commands;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import adminServer.mvp.server.ManagedServer;
import adminServer.mvp.server.ManagedServerRepository;
import adminServer.mvp.user.User;
import adminServer.mvp.audit.AuditService;
import adminServer.mvp.company.CompanyMemberRepository;
import adminServer.mvp.user.UserRole;

@Service
public class AgentCommandService {
    private final AgentCommandRepository commands;
    private final ManagedServerRepository servers;
    private final ObjectMapper json;
    private final AuditService audit;
    private final CompanyMemberRepository members;
    public AgentCommandService(AgentCommandRepository commands, ManagedServerRepository servers, ObjectMapper json, AuditService audit, CompanyMemberRepository members) {
        this.commands = commands; this.servers = servers; this.json = json; this.audit = audit; this.members = members;
    }
    @Transactional
    public CommandView request(UUID serverId, User user, AgentCommandType type, Object payload) {
        ManagedServer server = servers.findByIdAndOwnerId(serverId, user.getId()).orElseThrow(NoSuchElementException::new);
        UserRole role = members.findByCompanyIdAndUserId(server.getCompany().getId(), user.getId()).orElseThrow().getRole();
        if (role == UserRole.VIEWER && !Set.of(AgentCommandType.LIST_PROCESSES, AgentCommandType.LIST_SERVICES,
                AgentCommandType.LIST_CONTAINERS, AgentCommandType.CONTAINER_LOGS, AgentCommandType.SYSTEM_LOGS,
                AgentCommandType.READ_FILE, AgentCommandType.CHECK_UPDATE).contains(type)) throw new SecurityException("Perfil visitante permite somente leitura");
        if (role != UserRole.ADMIN && Set.of(AgentCommandType.RUN_TERMINAL_COMMAND, AgentCommandType.WRITE_FILE).contains(type)) throw new SecurityException("Operação restrita a administradores do espaço");
        try { AgentCommand saved = commands.save(new AgentCommand(server, user, type, payload == null ? null : json.writeValueAsString(payload))); audit.record(user, type.name(), serverId, null, "commandId=" + saved.getId()); return view(saved); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Payload inválido", exception); }
    }
    @Transactional
    public Optional<AgentCommandResponse> claimNext(UUID serverId) {
        var pending = commands.findNextPending(serverId, PageRequest.of(0, 1));
        if (pending.isEmpty()) return Optional.empty();
        AgentCommand command = pending.getFirst(); Instant now = Instant.now(); command.claim(now);
        return Optional.of(new AgentCommandResponse(command.getId(), command.getType(), command.getPayload(), command.getCreatedAt(), now.plusSeconds(45)));
    }
    @Transactional
    public void complete(UUID serverId, UUID commandId, CompletionRequest request) {
        AgentCommand command = commands.findByIdAndServerId(commandId, serverId).orElseThrow(NoSuchElementException::new);
        if (command.getStatus() != AgentCommandStatus.CLAIMED) throw new IllegalStateException("Comando não está em execução");
        command.complete(request.success(), request.result(), request.errorMessage(), Instant.now());
    }
    @Transactional(readOnly = true)
    public CommandView get(UUID id, UUID serverId, UUID ownerId) { return view(commands.findAccessible(id, serverId, ownerId).orElseThrow(NoSuchElementException::new)); }
    @Transactional(readOnly = true)
    public List<CommandView> list(UUID serverId, UUID ownerId) { return commands.findAccessibleHistory(serverId, ownerId, PageRequest.of(0,50)).stream().map(this::view).toList(); }
    private CommandView view(AgentCommand c) { return new CommandView(c.getId(), c.getType(), c.getStatus(), c.getCreatedAt(), c.getCompletedAt(), c.getResult(), c.getErrorMessage()); }
    public record CompletionRequest(boolean success, String result, String errorMessage) { }
    public record CommandView(UUID id, AgentCommandType type, AgentCommandStatus status, Instant createdAt, Instant completedAt, String result, String errorMessage) { }
}
