package adminServer.mvp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthRateLimitService {
    private static final String LOGIN_SCOPE = "LOGIN_IP";
    private static final String REGISTER_SCOPE = "REGISTER_IP";
    private static final int MAX_LOGIN_FAILURES = 10;
    private static final int MAX_REGISTRATIONS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final AuthRateLimitRepository limits;

    public AuthRateLimitService(AuthRateLimitRepository limits) {
        this.limits = limits;
    }

    @Transactional
    public void assertLoginAllowed(String ipAddress) {
        Instant now = Instant.now();
        assertNotBlocked(rateLimit(LOGIN_SCOPE, ipAddress, now), now);
    }

    @Transactional(noRollbackFor = TooManyRequestsException.class)
    public void recordFailedLogin(String ipAddress) {
        Instant now = Instant.now();
        AuthRateLimit limit = rateLimit(LOGIN_SCOPE, ipAddress, now);
        resetWindowIfExpired(limit, now);
        assertNotBlocked(limit, now);

        int attempts = limit.getAttempts() + 1;
        limit.setAttempts(attempts);
        if (attempts >= MAX_LOGIN_FAILURES) {
            limit.setBlockedUntil(now.plus(BLOCK_DURATION));
            limit.setAttempts(0);
            throw blocked(limit, now);
        }
    }

    @Transactional(noRollbackFor = TooManyRequestsException.class)
    public void consumeRegistrationAttempt(String ipAddress) {
        Instant now = Instant.now();
        AuthRateLimit limit = rateLimit(REGISTER_SCOPE, ipAddress, now);
        resetWindowIfExpired(limit, now);
        assertNotBlocked(limit, now);

        if (limit.getAttempts() >= MAX_REGISTRATIONS) {
            limit.setBlockedUntil(now.plus(BLOCK_DURATION));
            throw blocked(limit, now);
        }
        limit.setAttempts(limit.getAttempts() + 1);
    }

    private AuthRateLimit rateLimit(String scope, String ipAddress, Instant now) {
        String ipHash = hashIp(ipAddress);
        return limits.findByScopeAndIpHash(scope, ipHash)
                .orElseGet(() -> limits.save(new AuthRateLimit(scope, ipHash, now)));
    }

    private void resetWindowIfExpired(AuthRateLimit limit, Instant now) {
        if (limit.getWindowStartedAt().plus(WINDOW).isBefore(now)) {
            limit.setAttempts(0);
            limit.setWindowStartedAt(now);
            limit.setBlockedUntil(null);
        }
    }

    private void assertNotBlocked(AuthRateLimit limit, Instant now) {
        if (limit.getBlockedUntil() != null && limit.getBlockedUntil().isAfter(now)) {
            throw blocked(limit, now);
        }
    }

    private TooManyRequestsException blocked(AuthRateLimit limit, Instant now) {
        return new TooManyRequestsException(Duration.between(now, limit.getBlockedUntil()).toSeconds());
    }

    private String hashIp(String ipAddress) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel calcular o identificador de origem", exception);
        }
    }
}
