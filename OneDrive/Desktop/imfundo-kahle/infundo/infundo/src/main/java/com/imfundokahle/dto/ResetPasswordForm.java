package com.imfundokahle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO para el paso 2 de recuperacion de contrasena: codigo + contrasena nueva. */
public class ResetPasswordForm {

    /** Minimo 8 caracteres, con mayuscula, minuscula, numero y caracter especial. */
    private static final String STRONG_PASSWORD_REGEXP =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$";

    @NotBlank
    @Email
    private String email;

    /** Token firmado generado en el paso 1 (ver PasswordResetService). */
    @NotBlank
    private String token;

    @NotBlank(message = "{validation.resetCode.required}")
    private String code;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100, message = "{validation.password.size}")
    @Pattern(regexp = STRONG_PASSWORD_REGEXP, message = "{validation.password.weak}")
    private String password;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
