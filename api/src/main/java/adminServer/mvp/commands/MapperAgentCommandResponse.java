package adminServer.mvp.commands;

import java.time.Instant;

public class MapperAgentCommandResponse {

    public AgentCommandResponse mapResponse(AgentCommand command, Instant timestampInicial, Instant timeStampFinal) {

        return new AgentCommandResponse(command.getId(), command.getType(), command.getPayload(), timestampInicial,
                timeStampFinal);

    }

}
