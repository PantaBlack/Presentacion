package com.imfundokahle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad Usuario: representa a administradores, profesores y alumnos.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Idioma preferido del usuario (para la interfaz). */
    private String preferredLanguage = "es";

    /** URL o inicial de la foto de perfil. */
    private String profilePicture;

    /** Si es false, la cuenta no puede iniciar sesion (deshabilitada por un administrador). */
    @Column(nullable = false)
    private boolean enabled = true;

    /** Intentos de inicio de sesion fallidos consecutivos, para el bloqueo temporal. */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /** Si no es nulo y es futuro, la cuenta esta bloqueada temporalmente por fuerza bruta. */
    private LocalDateTime lockedUntil;

    /** Momento del ultimo inicio de sesion exitoso (auditoria). */
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public User() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Devuelve el nombre completo del usuario. */
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Devuelve la inicial del nombre para el avatar. */
    @Transient
    public String getInitial() {
        return (firstName != null && !firstName.isEmpty())
                ? firstName.substring(0, 1).toUpperCase() : "?";
    }

    // ---- Getters y Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    /** True si la cuenta esta temporalmente bloqueada por intentos fallidos. */
    @Transient
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
