package com.imfundokahle.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;

/**
 * Codigo de recuperacion de contrasena, SIN ESTADO en el servidor: el codigo
 * de 6 digitos que se envia por correo nunca se guarda en la sesion ni en
 * base de datos. En su lugar, el
 * formulario de "nueva contrasena" viaja con un token firmado (HMAC-SHA256)
 * que contiene el correo, el codigo y una fecha de expiracion. Esto evita
 * tener que crear una tabla nueva solo para tokens de un solo uso de corta
 * duracion.
 */
@Service
public class PasswordResetService {

    /** Tiempo maximo que un codigo de recuperacion sigue siendo valido. */
    private static final long EXPIRY_SECONDS = 15 * 60;

    private final Random random = new SecureRandom();
    private final Mac hmac;

    public PasswordResetService() {
        try {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(key, "HmacSHA256"));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar el firmado del token de recuperacion", e);
        }
    }

    /** Codigo generado junto con su token firmado, listo para mandar por correo y para el formulario. */
    public static class ResetCode {
        public final String code;
        public final String token;

        public ResetCode(String code, String token) {
            this.code = code;
            this.token = token;
        }
    }

    /** Genera un codigo de 6 digitos y su token firmado para el correo dado. */
    public ResetCode generate(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        String token = sign(email.trim().toLowerCase(), code);
        return new ResetCode(code, token);
    }

    /** Firma el correo y el codigo junto a una fecha de expiracion. */
    private synchronized String sign(String email, String code) {
        long expiry = Instant.now().getEpochSecond() + EXPIRY_SECONDS;
        String payload = email + ":" + code + ":" + expiry;
        String payloadB64 = base64(payload.getBytes(StandardCharsets.UTF_8));
        String signature = base64(hmac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8)));
        return payloadB64 + "." + signature;
    }

    /**
     * Valida que el token no fue alterado, no vencio, y que el correo y el
     * codigo escritos por el usuario coinciden con los que viajan firmados.
     */
    public boolean validate(String token, String email, String code) {
        if (token == null || email == null || code == null) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot < 0) {
            return false;
        }
        String payloadB64 = token.substring(0, dot);
        String providedSignature = token.substring(dot + 1);

        String expectedSignature;
        synchronized (this) {
            expectedSignature = base64(hmac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8)));
        }
        if (!constantTimeEquals(expectedSignature, providedSignature)) {
            return false;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            String[] parts = payload.split(":", 3);
            if (parts.length != 3) {
                return false;
            }
            String storedEmail = parts[0];
            String storedCode = parts[1];
            long expiry = Long.parseLong(parts[2]);

            if (Instant.now().getEpochSecond() > expiry) {
                return false;
            }
            return storedEmail.equals(email.trim().toLowerCase()) && storedCode.equals(code.trim());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Comparacion en tiempo constante para no filtrar informacion por temporizacion. */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
