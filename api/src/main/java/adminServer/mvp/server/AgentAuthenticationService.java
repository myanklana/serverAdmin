package adminServer.mvp.server;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AgentAuthenticationService {
    private final ManagedServerRepository servers;
    private final PasswordEncoder passwordEncoder;
    public AgentAuthenticationService(ManagedServerRepository servers, PasswordEncoder passwordEncoder) { this.servers = servers; this.passwordEncoder = passwordEncoder; }
    public ManagedServer authenticate(String token) {
        return servers.findByTokenLookupHash(lookupHash(token)).filter(server -> passwordEncoder.matches(token, server.getTokenHash()))
                .orElseThrow(() -> new SecurityException("Token de agente inválido"));
    }
    private String lookupHash(String token) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
