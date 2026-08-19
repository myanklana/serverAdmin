package adminServer.mvp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AgentAuthenticationServiceTest {
    private static final String TOKEN = "12345678901234567890123456789012";

    @Mock private ManagedServerRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ManagedServer server;

    @Test
    void authenticatesUsingLookupHashAndPasswordHash() throws Exception {
        String lookupHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(TOKEN.getBytes(StandardCharsets.UTF_8)));
        when(repository.findByTokenLookupHash(lookupHash)).thenReturn(Optional.of(server));
        when(server.getTokenHash()).thenReturn("stored-hash");
        when(passwordEncoder.matches(TOKEN, "stored-hash")).thenReturn(true);

        ManagedServer authenticated = new AgentAuthenticationService(repository, passwordEncoder).authenticate(TOKEN);

        assertThat(authenticated).isSameAs(server);
        verify(repository).findByTokenLookupHash(lookupHash);
    }

    @Test
    void rejectsTokenWhenPasswordHashDoesNotMatch() {
        when(repository.findByTokenLookupHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(server));
        when(server.getTokenHash()).thenReturn("stored-hash");
        when(passwordEncoder.matches(TOKEN, "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> new AgentAuthenticationService(repository, passwordEncoder).authenticate(TOKEN))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void rejectsUnknownLookupHashWithoutCheckingPassword() {
        when(repository.findByTokenLookupHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AgentAuthenticationService(repository, passwordEncoder).authenticate(TOKEN))
                .isInstanceOf(SecurityException.class);

        verify(passwordEncoder, org.mockito.Mockito.never())
                .matches(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
