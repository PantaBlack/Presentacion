package com.imfundokahle.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Valida que un correo tenga formato correcto y pertenezca a gmail.com/googlemail.com.
 * La validacion de formato (@Email) es responsabilidad de Bean Validation;
 * aqui solo se restringe el dominio permitido.
 */
public class GmailAddressValidator implements ConstraintValidator<GmailAddress, String> {

    private static final Pattern GMAIL_DOMAIN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@(gmail\\.com|googlemail\\.com)$");

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            // La ausencia de valor la reporta @NotBlank; esta validacion no duplica el error.
            return true;
        }
        return GMAIL_DOMAIN.matcher(email.trim().toLowerCase(Locale.ROOT)).matches();
    }
}
