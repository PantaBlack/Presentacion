package com.imfundokahle.service;

import com.imfundokahle.model.Role;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de usuarios. Implementa UserDetailsService para Spring Security
 * e incluye la logica de bloqueo de cuenta por intentos fallidos.
 */
@Service
public class UserService implements UserDetailsService {

    /** Intentos fallidos consecutivos permitidos antes de bloquear la cuenta. */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    /** Duracion del bloqueo temporal tras superar el limite de intentos. */
    public static final long LOCK_DURATION_MINUTES = 15;

    /** Roles que un usuario puede elegir libremente al auto-registrarse. */
    private static final List<Role> SELF_REGISTERABLE_ROLES = List.of(Role.STUDENT, Role.TEACHER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo usuario desde el formulario publico.
     * Por seguridad, el auto-registro NUNCA puede producir una cuenta ADMIN
     * ni PRACTICANTE: esos roles solo los asigna un administrador ya autenticado
     * desde /admin/users.
     */
    @Transactional
    public User register(User user, Role selectedRole) {
        if (!SELF_REGISTERABLE_ROLES.contains(selectedRole)) {
            throw new IllegalArgumentException("Rol no permitido para auto-registro: " + selectedRole);
        }
        user.setRole(selectedRole);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    /** Crea un usuario ya con rol asignado y contrasena en texto plano (uso interno / seed). */
    @Transactional
    public User createUser(String firstName, String lastName, String email,
                           String rawPassword, Role role) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    /** Total de usuarios registrados (todos los roles). */
    public long countAll() {
        return userRepository.count();
    }

    public List<User> findRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional
    public void updateRole(Long userId, Role newRole) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setRole(newRole);
            userRepository.save(u);
        });
    }

    /** Habilita o deshabilita una cuenta (control de acceso desde el panel admin). */
    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setEnabled(enabled);
            if (enabled) {
                u.setFailedLoginAttempts(0);
                u.setLockedUntil(null);
            }
            userRepository.save(u);
        });
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /** Establece una nueva contrasena (recuperacion via correo); no hace nada si el correo no existe. */
    @Transactional
    public void resetPassword(String email, String newRawPassword) {
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode(newRawPassword));
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            userRepository.save(u);
        });
    }

    @Transactional
    public void updateProfile(User user, String firstName, String lastName, String preferredLanguage) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPreferredLanguage(preferredLanguage);
        userRepository.save(user);
    }

    // ---- Control de fuerza bruta ----

    /** Registra un intento de login fallido; bloquea la cuenta si se supera el limite. */
    @Transactional
    public void registerFailedLogin(String email) {
        if (email == null) return;
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(u -> {
            int attempts = u.getFailedLoginAttempts() + 1;
            u.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            }
            userRepository.save(u);
        });
    }

    /** Limpia el contador de intentos fallidos tras un login exitoso. */
    @Transactional
    public void registerSuccessfulLogin(String email) {
        if (email == null) return;
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(u -> {
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            u.setLastLoginAt(LocalDateTime.now());
            userRepository.save(u);
        });
    }

    /** Carga el usuario para autenticacion de Spring Security (correo normalizado, sin distinguir mayusculas). */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + normalized));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .disabled(!user.isEnabled())
                .accountLocked(user.isLocked())
                .accountExpired(false)
                .credentialsExpired(false)
                .build();
    }
}
