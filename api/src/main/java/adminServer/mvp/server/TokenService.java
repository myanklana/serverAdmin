package adminServer.mvp.server;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.SecretKey;

import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final SecureRandom random = new SecureRandom();
    private PasswordEncoder passwordEncoder;
    private SecretKey key;

    public String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashParaVerificar(String rawToken) {
        return passwordEncoder.encode(rawToken);
    }

    public String keyHash(String rawToken) {
        if (key == null) {
            throw new IllegalStateException("Key not initialized");
        }
        return HexFormat.of()
                .formatHex(HmacUtils.hmacSha256(key.getEncoded(), rawToken.getBytes(StandardCharsets.UTF_8)));
    }

}
