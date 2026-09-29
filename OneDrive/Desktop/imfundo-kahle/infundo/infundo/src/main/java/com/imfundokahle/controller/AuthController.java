package com.imfundokahle.controller;

import com.imfundokahle.dto.ForgotPasswordForm;
import com.imfundokahle.dto.RegisterForm;
import com.imfundokahle.dto.ResetPasswordForm;
import com.imfundokahle.model.Role;
import com.imfundokahle.model.User;
import com.imfundokahle.service.MailService;
import com.imfundokahle.service.PasswordResetService;
import com.imfundokahle.service.RecaptchaService;
import com.imfundokahle.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de autenticacion: login, registro, recuperacion de contrasena.
 * El procesamiento del login (POST) lo maneja Spring Security; el reCAPTCHA
 * de esa pantalla se valida antes, en {@link com.imfundokahle.security.CaptchaFilter}.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final RecaptchaService recaptchaService;
    private final PasswordResetService passwordResetService;
    private final MailService mailService;

    public AuthController(UserService userService, RecaptchaService recaptchaService,
                          PasswordResetService passwordResetService, MailService mailService) {
        this.userService = userService;
        this.recaptchaService = recaptchaService;
        this.passwordResetService = passwordResetService;
        this.mailService = mailService;
    }

    /** Muestra el formulario de inicio de sesion (el widget de reCAPTCHA se pinta con recaptchaSiteKey). */
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /** Muestra el formulario de registro. */
    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    /** Procesa el registro de un nuevo usuario. */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult result,
                           @RequestParam(name = "g-recaptcha-response", required = false) String recaptchaResponse,
                           RedirectAttributes redirectAttributes) {

        // Normalizar el correo (evita duplicados por diferencias de mayusculas)
        if (form.getEmail() != null) {
            form.setEmail(form.getEmail().trim().toLowerCase());
        }

        if (!recaptchaService.validate(recaptchaResponse)) {
            result.reject("captcha.error", "CAPTCHA incorrecto");
        }
        // Validar coincidencia de contrasenas
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Las contrasenas no coinciden");
        }
        // Validar email unico
        if (form.getEmail() != null && userService.emailExists(form.getEmail())) {
            result.rejectValue("email", "email.exists", "El correo ya esta registrado");
        }

        if (result.hasErrors()) {
            return "auth/register";
        }

        // Crear usuario. El DTO ya garantiza (via @Pattern) que el rol es STUDENT o TEACHER;
        // UserService.register() rechaza explicitamente cualquier otro rol como defensa en profundidad.
        User user = new User();
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(form.getEmail());
        user.setPassword(form.getPassword());

        Role selectedRole = Role.valueOf(form.getRole());
        userService.register(user, selectedRole);

        redirectAttributes.addFlashAttribute("successKey", "registro.exito");
        return "redirect:/auth/login";
    }

    /** Paso 1 de recuperacion de contrasena: pedir el correo. */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    /**
     * Genera el codigo y lo manda por correo. Por seguridad, la respuesta es
     * identica exista o no una cuenta con ese correo (evita que alguien use
     * este formulario para averiguar que correos estan registrados): siempre
     * se redirige al paso 2 con un token firmado para ese correo, pero el
     * correo real solo se envia si la cuenta existe de verdad.
     */
    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                                 BindingResult result,
                                 @RequestParam(name = "g-recaptcha-response", required = false) String recaptchaResponse,
                                 RedirectAttributes redirectAttributes) {
        if (form.getEmail() != null) {
            form.setEmail(form.getEmail().trim().toLowerCase());
        }
        if (!recaptchaService.validate(recaptchaResponse)) {
            result.reject("captcha.error", "CAPTCHA incorrecto");
        }
        if (result.hasErrors()) {
            return "auth/forgot-password";
        }

        PasswordResetService.ResetCode resetCode = passwordResetService.generate(form.getEmail());
        userService.findByEmail(form.getEmail())
                .ifPresent(u -> mailService.enviarCodigoRecuperacion(form.getEmail(), resetCode.code));

        redirectAttributes.addFlashAttribute("successKey", "forgot.enviado");
        redirectAttributes.addAttribute("email", form.getEmail());
        redirectAttributes.addAttribute("token", resetCode.token);
        return "redirect:/auth/reset-password";
    }

    /** Paso 2: escribir el codigo recibido y la contrasena nueva. */
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String email,
                                    @RequestParam(required = false) String token, Model model) {
        if (email == null || token == null) {
            return "redirect:/auth/forgot-password";
        }
        if (!model.containsAttribute("resetPasswordForm")) {
            ResetPasswordForm form = new ResetPasswordForm();
            form.setEmail(email);
            form.setToken(token);
            model.addAttribute("resetPasswordForm", form);
        }
        return "auth/reset-password";
    }

    /** Valida el codigo y, si es correcto, guarda la contrasena nueva. */
    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                BindingResult result,
                                @RequestParam(name = "g-recaptcha-response", required = false) String recaptchaResponse,
                                RedirectAttributes redirectAttributes) {
        // Sin este captcha, el codigo de 6 digitos (1 en 1,000,000) se podria intentar
        // adivinar por fuerza bruta de forma automatizada dentro de los 15 minutos que
        // el token sigue siendo valido.
        if (!recaptchaService.validate(recaptchaResponse)) {
            result.reject("captcha.error", "CAPTCHA incorrecto");
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Las contrasenas no coinciden");
        }
        if (!passwordResetService.validate(form.getToken(), form.getEmail(), form.getCode())) {
            result.rejectValue("code", "resetCode.invalid", "Codigo invalido o vencido");
        }

        if (result.hasErrors()) {
            return "auth/reset-password";
        }

        userService.resetPassword(form.getEmail(), form.getPassword());
        redirectAttributes.addFlashAttribute("successKey", "reset.exito");
        return "redirect:/auth/login";
    }
}
