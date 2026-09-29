package com.imfundokahle.controller;

import com.imfundokahle.model.Role;
import com.imfundokahle.model.User;
import com.imfundokahle.service.ActividadService;
import com.imfundokahle.service.ContentImageService;
import com.imfundokahle.service.MotivationalQuoteService;
import com.imfundokahle.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Advice global que expone datos comunes a todas las vistas: el usuario
 * autenticado (para navbar, avatar, saludo) y, para profesores, alumnos y
 * practicantes, cuantas actividades pendientes tiene asignadas el
 * administrador (para el aviso en la barra lateral).
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Set<Role> ROLES_CON_ACTIVIDADES =
            EnumSet.of(Role.TEACHER, Role.STUDENT, Role.PRACTICANTE);

    private final UserService userService;
    private final ActividadService actividadService;
    private final ContentImageService contentImageService;
    private final MotivationalQuoteService quoteService;

    @Value("${app.recaptcha.site-key}")
    private String recaptchaSiteKey;

    public GlobalModelAdvice(UserService userService, ActividadService actividadService,
                              ContentImageService contentImageService, MotivationalQuoteService quoteService) {
        this.userService = userService;
        this.actividadService = actividadService;
        this.contentImageService = contentImageService;
        this.quoteService = quoteService;
    }

    /** Frase motivacional aleatoria segun el rol, para el saludo del panel de cada quien. */
    @ModelAttribute("motivationalQuote")
    public String motivationalQuote(Principal principal) {
        if (principal == null) return "";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "";
        switch (user.getRole()) {
            case STUDENT: return quoteService.getRandomStudentQuote();
            case TEACHER: return quoteService.getRandomTeacherQuote();
            case PRACTICANTE: return quoteService.getRandomPracticanteQuote();
            default: return "";
        }
    }

    /** Version actual de cada imagen editable (para el "?v="); disponible en cualquier vista
     *  sin que cada controlador tenga que pedirla. index.html y admin/contenido.html ya la
     *  agregaban por su cuenta con el mismo nombre; eso sigue funcionando, simplemente
     *  vuelve a poner el mismo valor. */
    @ModelAttribute("imgV")
    public Map<String, Long> imgV() {
        return contentImageService.getVersiones();
    }

    /** Clave publica de reCAPTCHA, disponible en cualquier vista que necesite el widget. */
    @ModelAttribute("recaptchaSiteKey")
    public String recaptchaSiteKey() {
        return recaptchaSiteKey;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findByEmail(principal.getName()).orElse(null);
    }

    /** Actividades pendientes de revisar: alimenta el aviso en la barra lateral. */
    @ModelAttribute("pendingActivitiesCount")
    public long pendingActivitiesCount(Principal principal) {
        if (principal == null) {
            return 0;
        }
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null || !ROLES_CON_ACTIVIDADES.contains(user.getRole())) {
            return 0;
        }
        return actividadService.obtenerResumenPara(user.getId()).pendientes();
    }
}
