package com.imfundokahle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuracion aislada del codificador de contrasenas.
 * Se separa de SecurityConfig para evitar un ciclo de dependencias
 * (SecurityConfig -> UserService -> PasswordEncoder).
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
