package com.imfundokahle.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que el correo pertenezca al dominio gmail.com (o googlemail.com).
 * El centro solo acepta altas con una cuenta de Gmail real, como control
 * adicional de identidad para el auto-registro.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GmailAddressValidator.class)
public @interface GmailAddress {

    String message() default "{validation.email.gmail}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
