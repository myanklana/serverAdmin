package adminServer.mvp.commands;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.weaver.loadtime.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import jakarta.transaction.Transactional;

public class AgentCommandService {

    @Autowired
    private AgentCommandRepository agentCommandRepository;

    @Autowired
    private MapperAgentCommandResponse mapper;

    @Transactional
    // Executa proximo comando
    public Optional<AgentCommandResponse> claimNext(UUID serverId) {
        var commmands = agentCommandRepository.findNextPending(serverId, PageRequest.of(0, 1));
        var clock = Instant.now();

        if (commmands.isEmpty()) {
            return Optional.empty();
        }

        var command = commmands.getFirst();
        claimed(command, clock);
        var timeStampFim = clock.plusSeconds(30);

        return Optional.of(mapper.mapResponse(command, clock, timeStampFim));
    }

    private void claimed(AgentCommand command, Instant claimed) {

        agentCommandRepository.updateCommandStatus(AgentCommandStatus.CLAIMED, claimed, command.getId());

    }

}
