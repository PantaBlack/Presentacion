package com.imfundokahle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO para el paso 1 de recuperacion de contrasena: solo el correo. */
public class ForgotPasswordForm {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
