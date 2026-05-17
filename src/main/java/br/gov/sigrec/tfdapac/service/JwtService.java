package br.gov.sigrec.tfdapac.service;

import br.gov.sigrec.tfdapac.repository.UserAccountRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;
    private final String secret;
    private final long expirationMinutes;

    public JwtService(ObjectMapper objectMapper,
                      UserAccountRepository userAccountRepository,
                      @Value("${sigrec.jwt-secret}") String secret,
                      @Value("${sigrec.jwt-expiration-minutes}") long expirationMinutes) {
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(String username) {
        var user = userAccountRepository.findByUsername(username).orElseThrow();
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("role", user.getRole().name());
        payload.put("uid", user.getId());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(expirationMinutes * 60).getEpochSecond());
        String unsigned = encode(header) + "." + encode(payload);
        return unsigned + "." + sign(unsigned);
    }

    public Optional<JwtUser> validate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigestUtil.constantTimeEquals(sign(unsigned), parts[2])) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {
            });
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }
            String username = payload.get("sub").toString();
            String role = payload.get("role").toString();
            Long userId = ((Number) payload.get("uid")).longValue();
            return Optional.of(new JwtUser(userId, username, role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String encode(Map<String, Object> data) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(data));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao montar JWT", e);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar JWT", e);
        }
    }

    public record JwtUser(Long id, String username, String role) {
    }

    private static class MessageDigestUtil {
        static boolean constantTimeEquals(String left, String right) {
            byte[] a = left.getBytes(StandardCharsets.UTF_8);
            byte[] b = right.getBytes(StandardCharsets.UTF_8);
            if (a.length != b.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
