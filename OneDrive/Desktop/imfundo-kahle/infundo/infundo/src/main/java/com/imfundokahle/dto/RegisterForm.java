package com.imfundokahle.dto;

import com.imfundokahle.validation.GmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario de registro publico, con validaciones.
 * Solo permite auto-registro como STUDENT o TEACHER: los roles
 * PRACTICANTE y ADMIN unicamente los asigna un administrador.
 */
public class RegisterForm {

    /** Solo letras (con acentos/nn), espacios, guiones y apostrofes: nada de numeros ni simbolos. */
    private static final String NAME_REGEXP = "^[\\p{L} '-]+$";

    /** Minimo 8 caracteres, con mayuscula, minuscula, numero y caracter especial. */
    private static final String STRONG_PASSWORD_REGEXP =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$";

    @NotBlank(message = "{validation.firstName.required}")
    @Size(max = 80, message = "{validation.firstName.size}")
    @Pattern(regexp = NAME_REGEXP, message = "{validation.name.invalid}")
    private String firstName;

    @NotBlank(message = "{validation.lastName.required}")
    @Size(max = 80, message = "{validation.lastName.size}")
    @Pattern(regexp = NAME_REGEXP, message = "{validation.name.invalid}")
    private String lastName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @GmailAddress
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100, message = "{validation.password.size}")
    @Pattern(regexp = STRONG_PASSWORD_REGEXP, message = "{validation.password.weak}")
    private String password;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;

    /** Rol seleccionado: unicamente STUDENT o TEACHER. */
    @NotBlank(message = "{validation.role.required}")
    @Pattern(regexp = "STUDENT|TEACHER", message = "{validation.role.invalid}")
    private String role;

    // Getters y Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
