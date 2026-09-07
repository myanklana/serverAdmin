package adminServer.mvp.security;
import java.util.UUID;
import adminServer.mvp.user.UserRole;
public record AuthenticatedUser(UUID id, String username, UserRole role) {
    public AuthenticatedUser(UUID id, String username) { this(id, username, UserRole.ADMIN); }
}
