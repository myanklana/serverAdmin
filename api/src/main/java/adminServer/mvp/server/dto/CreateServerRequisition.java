package adminServer.mvp.server.dto;

import adminServer.mvp.server.ServerController.CreateServerRequest;
import adminServer.mvp.user.User;

public record CreateServerRequisition(String name, String host, String ip, int port) {
    public static CreateServerRequisition from(CreateServerRequest req) {
        return new CreateServerRequisition(req.name(), req.hostname(), req.ip(), req.port());

    }
}
