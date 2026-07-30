package adminServer.mvp.auth;

import adminServer.mvp.user.User;
import adminServer.mvp.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import adminServer.mvp.security.AuthenticatedUser;
import java.util.Locale;

import java.time.Instant;

@RestController
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody Credentials request) {
        String username = normalizeUsername(request.username());
        if (users.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = users.save(new User(username, passwordEncoder.encode(request.password())));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody Credentials request) {
        User user = users.findByUsername(normalizeUsername(request.username()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException());
        return new TokenResponse(jwtService.createToken(user), "Bearer", jwtService.expiresAt());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = users.findById(((AuthenticatedUser) authentication.getPrincipal()).id()).orElseThrow();
        return UserResponse.from(user);
    }

    public record Credentials(@NotBlank @Size(min = 3, max = 80) String username,
                              @NotBlank @Size(min = 8, max = 100) String password) { }
    public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) { }
    public record UserResponse(java.util.UUID id, String username, Instant createdAt) {
        static UserResponse from(User user) { return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt()); }
    }
    private String normalizeUsername(String username) { return username.trim().toLowerCase(Locale.ROOT); }
}
