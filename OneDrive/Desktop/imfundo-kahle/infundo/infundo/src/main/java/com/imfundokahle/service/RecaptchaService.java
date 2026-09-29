package com.imfundokahle.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Verifica el widget de Google reCAPTCHA v2 ("No soy un robot") contra la API
 * oficial de Google (nunca confiando en nada que mande el propio navegador):
 * reemplaza al CAPTCHA matematico casero en login, registro y recuperacion
 * de contrasena.
 */
@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String secretKey;

    public RecaptchaService(@Value("${app.recaptcha.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    /** True solo si Google confirma que la persona de verdad resolvio el widget. */
    public boolean validate(String gRecaptchaResponse) {
        if (gRecaptchaResponse == null || gRecaptchaResponse.isBlank()) {
            return false;
        }
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("RECAPTCHA_SECRET_KEY no esta configurada: se rechaza el intento por seguridad " +
                    "(mejor negar el acceso que dejarlo pasar sin verificar nada).");
            return false;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secretKey);
        body.add("response", gRecaptchaResponse);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            VerifyResponse resp = restTemplate.postForObject(
                    VERIFY_URL, new HttpEntity<>(body, headers), VerifyResponse.class);
            return resp != null && resp.success;
        } catch (RestClientException e) {
            log.error("No se pudo verificar el reCAPTCHA con Google: {}", e.getMessage());
            return false;
        }
    }

    /** Solo nos interesa el campo "success" de la respuesta de Google. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VerifyResponse {
        public boolean success;
    }
}
