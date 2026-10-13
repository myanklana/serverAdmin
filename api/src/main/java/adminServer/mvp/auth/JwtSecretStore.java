package adminServer.mvp.auth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Keeps a randomly generated JWT signing key on disk, so a local installation
 * works without manual secret setup and tokens survive restarts.
 */
final class JwtSecretStore {
    private JwtSecretStore() {
    }

    static String loadOrCreate(Path file) {
        try {
            if (Files.isRegularFile(file)) {
                return Files.readString(file).trim();
            }
            byte[] bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
            String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, secret);
            try {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX file system (Windows): rely on the directory ACL.
            }
            return secret;
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível ler ou gravar o segredo JWT em " + file, exception);
        }
    }
}
