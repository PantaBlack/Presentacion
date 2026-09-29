package com.imfundokahle.config;

import com.imfundokahle.security.CaptchaFilter;
import com.imfundokahle.service.RecaptchaService;
import com.imfundokahle.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuracion de Spring Security.
 * <p>
 * Puntos clave de este endurecimiento:
 * <ul>
 *   <li>La consola H2 esta apagada salvo que se active explicitamente con
 *       {@code spring.h2.console.enabled=true} (perfil dev). Dejarla abierta en
 *       produccion permitia ejecutar SQL arbitrario y auto-otorgarse el rol ADMIN
 *       sin pasar por ningun control de acceso: era la causa real de "poner la URL
 *       del admin y entrar".</li>
 *   <li>CSRF queda activo en TODA la aplicacion, incluida la API de actividades
 *       (antes exenta); el JS correspondiente envia el token via cabecera.</li>
 *   <li>Bloqueo temporal de cuenta tras varios intentos fallidos (fuerza bruta).</li>
 *   <li>Un usuario autenticado que visita una seccion de otro rol es redirigido
 *       a su propio panel en vez de ver un error tecnico.</li>
 *   <li>Maximo una sesion activa por usuario: iniciar sesion en un dispositivo
 *       nuevo invalida la sesion anterior.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private final UserService userService;
    private final RecaptchaService recaptchaService;
    private final PasswordEncoder passwordEncoder;

    /** Solo true en desarrollo local (application-dev.properties). Apagado = mas seguro por defecto. */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfig(UserService userService, RecaptchaService recaptchaService,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.recaptchaService = recaptchaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        // No revelar si la cuenta no existe o si la contrasena es incorrecta: mismo mensaje para ambas.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    /** Publica los eventos de sesion HTTP, necesarios para que maximumSessions() funcione correctamente. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /** A donde enviar a cada rol tras autenticarse (o al intentar entrar a una zona ajena). */
    private String homeFor(org.springframework.security.core.Authentication authentication) {
        Map<String, String> homes = new LinkedHashMap<>();
        homes.put("ROLE_ADMIN", "/admin/dashboard");
        homes.put("ROLE_TEACHER", "/teacher/dashboard");
        homes.put("ROLE_STUDENT", "/student/dashboard");
        homes.put("ROLE_PRACTICANTE", "/practicante/dashboard");
        return authentication.getAuthorities().stream()
                .map(a -> homes.get(a.getAuthority()))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse("/");
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            userService.registerSuccessfulLogin(authentication.getName());
            response.sendRedirect(request.getContextPath() + homeFor(authentication));
        };
    }

    /**
     * Registra el intento fallido (con bloqueo tras varios errores) y distingue
     * el mensaje que ve el usuario: credenciales invalidas, cuenta bloqueada o deshabilitada.
     */
    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            String email = request.getParameter("email");
            String reason;
            if (exception instanceof LockedException) {
                reason = "locked";
            } else if (exception instanceof DisabledException) {
                reason = "disabled";
            } else {
                userService.registerFailedLogin(email);
                reason = "true";
            }
            String url = UriComponentsBuilder.fromPath(request.getContextPath() + "/auth/login")
                    .queryParam("error", reason)
                    .build().toUriString();
            response.sendRedirect(url);
        };
    }

    /** Usuario autenticado sin permiso para la seccion visitada: lo mandamos a su propio panel. */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, org.springframework.security.access.AccessDeniedException ex) -> {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String home = (authentication != null) ? homeFor(authentication) : "/";
            response.sendRedirect(request.getContextPath() + home + "?accessDenied=true");
        };
    }

    /**
     * CSP a medida de lo que la app realmente carga: recursos propios, Google Fonts,
     * el CDN de three.js para la escena 3D del hero publico, y el widget de Google
     * reCAPTCHA (login/registro/recuperar contrasena). Todo lo demas -scripts, hojas
     * de estilo, iframes de otros origenes- queda bloqueado por defecto.
     * <p>
     * "unsafe-inline" en script/style sigue siendo necesario porque varias vistas
     * usan &lt;script&gt; y estilos en linea; quitarlo del todo requeriria pasar a
     * nonces por peticion (Thymeleaf + un filtro que los genere), un cambio mayor
     * que queda pendiente como endurecimiento futuro.
     */
    private String contentSecurityPolicy() {
        String frameAncestors = h2ConsoleEnabled ? "'self'" : "'none'";
        return "default-src 'self'; "
                + "base-uri 'self'; "
                + "object-src 'none'; "
                + "frame-ancestors " + frameAncestors + "; "
                + "frame-src https://www.google.com https://recaptcha.google.com; "
                + "form-action 'self'; "
                + "img-src 'self' data:; "
                + "font-src 'self' https://fonts.gstatic.com; "
                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                + "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com "
                + "https://www.google.com https://www.gstatic.com https://recaptcha.google.com; "
                + "connect-src 'self' https://www.google.com https://www.gstatic.com";
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/", "/index", "/lang/**", "/auth/**",
                        "/css/**", "/js/**", "/img/**", "/webjars/**", "/error",
                        "/contenido/imagen/**", "/contenido/seccion-imagen/**").permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth.requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/teacher/**").hasRole("TEACHER")
                    .requestMatchers("/student/**").hasRole("STUDENT")
                    .requestMatchers("/practicante/**").hasRole("PRACTICANTE")
                    // Disponibilidad es solo para quienes reciben tareas por hora (profesor/practicante);
                    // el alumno solo recibe clases y horarios ya asignados, no gestiona disponibilidad propia.
                    .requestMatchers("/mi-disponibilidad/**").hasAnyRole("TEACHER", "PRACTICANTE")
                    .requestMatchers("/mi-tiempo/**").hasAnyRole("TEACHER", "STUDENT", "PRACTICANTE")
                    .anyRequest().authenticated();
            })
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler())
                .failureHandler(failureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()))
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .headers(headers -> headers
                // Sin la consola H2 no hay ninguna vista legitima que necesite ser enmarcada.
                .frameOptions(frame -> { if (h2ConsoleEnabled) frame.sameOrigin(); else frame.deny(); })
                .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy()))
                // Solo tiene efecto cuando la conexion ya es HTTPS (asi lo decide Spring
                // Security por si mismo); en HTTP plano de desarrollo no hace nada.
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        if (h2ConsoleEnabled) {
            // La consola H2 hace sus propias peticiones sin token CSRF; solo se exime en desarrollo.
            http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));
        }

        http.addFilterBefore(new CaptchaFilter(recaptchaService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
