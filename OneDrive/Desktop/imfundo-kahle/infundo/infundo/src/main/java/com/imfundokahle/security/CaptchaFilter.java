package com.imfundokahle.security;

import com.imfundokahle.service.RecaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Valida el widget de Google reCAPTCHA ANTES de que Spring Security procese
 * el inicio de sesion. Si no se resolvio (o Google lo rechaza), redirige de
 * vuelta al formulario de login con un error, sin llegar siquiera a intentar
 * autenticar las credenciales.
 */
public class CaptchaFilter extends OncePerRequestFilter {

    private final RecaptchaService recaptchaService;

    public CaptchaFilter(RecaptchaService recaptchaService) {
        this.recaptchaService = recaptchaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Solo interceptar el POST de inicio de sesion
        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/auth/login".equals(request.getServletPath())) {

            String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
            if (!recaptchaService.validate(gRecaptchaResponse)) {
                response.sendRedirect(request.getContextPath() + "/auth/login?captchaError=true");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
