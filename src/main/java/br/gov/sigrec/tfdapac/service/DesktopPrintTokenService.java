package br.gov.sigrec.tfdapac.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class DesktopPrintTokenService {
    private static final String SECRET_PROPERTY = "sigrec.desktop.print-token-secret";
    private static final long VALIDITY_SECONDS = 300;

    public static String createToken(Long solicitacaoId) {
        long expiresAt = Instant.now().getEpochSecond() + VALIDITY_SECONDS;
        return expiresAt + "." + sign(solicitacaoId, expiresAt);
    }

    public boolean isValid(Long solicitacaoId, String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            long expiresAt = Long.parseLong(parts[0]);
            if (Instant.now().getEpochSecond() > expiresAt) {
                return false;
            }

            String expected = sign(solicitacaoId, expiresAt);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String sign(Long solicitacaoId, long expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((solicitacaoId + ":" + expiresAt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel gerar token de impressao desktop", e);
        }
    }

    private static String secret() {
        String secret = System.getProperty(SECRET_PROPERTY);
        if (StringUtils.hasText(secret)) {
            return secret;
        }

        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        System.setProperty(SECRET_PROPERTY, secret);
        return secret;
    }
}
