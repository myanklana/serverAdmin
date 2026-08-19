package adminServer.mvp.security;
import java.util.UUID;
public record AuthenticatedUser(UUID id, String username) { }
