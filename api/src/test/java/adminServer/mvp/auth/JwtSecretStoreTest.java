package adminServer.mvp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwtSecretStoreTest {

    @Test
    void generatesStrongSecretOnceAndReusesIt(@TempDir Path dir) {
        Path file = dir.resolve("nested/jwt-secret");

        String first = JwtSecretStore.loadOrCreate(file);
        String second = JwtSecretStore.loadOrCreate(file);

        assertTrue(first.getBytes().length >= 32);
        assertEquals(first, second);
    }
}
