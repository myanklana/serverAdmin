package adminServer.mvp.websocket;

import java.security.Principal;
import java.util.UUID;

public record WebSocketPrincipal(
        UUID ownerId) implements Principal {
    @Override
    public String getName() {
        return ownerId.toString();
    }

}
