package adminServer.mvp.auth;

import adminServer.mvp.security.AuthenticatedUser;
import adminServer.mvp.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalArgumentException("APP_JWT_SECRET deve ter ao menos 32 bytes");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("username", user.getUsername()).issuer(issuer)
                .audience().add(audience).and().issuedAt(Date.from(now)).expiration(Date.from(expiresAt(now)))
                .signWith(key).compact();
    }

    public UUID userId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).requireIssuer(issuer).requireAudience(audience).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return UUID.fromString(subject);
        } catch (RuntimeException ex) {
            throw new InvalidTokenException();
        }
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = parseAndValidate(token);
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get("username", String.class));
    }

    private Claims parseAndValidate(String token) {
        try {
            return Jwts.parser().verifyWith(key).requireIssuer(issuer).requireAudience(audience).build()
                    .parseSignedClaims(token).getPayload();
        } catch (RuntimeException ex) {
            throw new InvalidTokenException();
        }
    }

    public Instant expiresAt() {
        return expiresAt(Instant.now());
    }

    private Instant expiresAt(Instant issuedAt) {
        return issuedAt.plusSeconds(expirationMinutes * 60);
    }
}
