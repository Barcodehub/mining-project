package com.mining.soft.infrastructure.privacy;

import com.mining.soft.application.spi.IPseudonymizerPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Seudonimización con HMAC-SHA256 y una clave secreta del despliegue (se trunca a 128 bits). */
@Component
public class HmacPseudonymizer implements IPseudonymizerPort {

    private final SecretKeySpec key;

    public HmacPseudonymizer(@Value("${privacidad.seudonimo.secreto:change-me-in-production}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String pseudonym(Long userId) {
        if (userId == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] h = mac.doFinal(userId.toString().getBytes(StandardCharsets.UTF_8));
            return "u_" + HexFormat.of().formatHex(h, 0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo seudonimizar", e);
        }
    }
}
