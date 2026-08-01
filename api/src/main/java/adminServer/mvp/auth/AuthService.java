package adminServer.mvp.auth;

import adminServer.mvp.user.User;
import adminServer.mvp.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = users.findByUsername(normalizeUsername(username));
        if (user.isEmpty()) {
            return Optional.empty();
        }

        User found = user.get();
        Instant now = Instant.now();
        if (found.getLockedUntil() != null) {
            if (found.getLockedUntil().isAfter(now)) {
                return Optional.empty();
            }
            found.setLockedUntil(null);
            found.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(password, found.getPasswordHash())) {
            int attempts = found.getFailedLoginAttempts() + 1;
            found.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                found.setLockedUntil(now.plus(ACCOUNT_LOCK_DURATION));
                found.setFailedLoginAttempts(0);
            }
            return Optional.empty();
        }

        found.setFailedLoginAttempts(0);
        found.setLockedUntil(null);
        return Optional.of(found);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
