package com.imfundokahle.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador para cambiar el idioma de la interfaz.
 * El cambio real lo realiza el LocaleChangeInterceptor con el parametro ?lang.
 * Este controlador simplemente redirige de vuelta a la pagina anterior.
 */
@Controller
public class LanguageController {

    @GetMapping("/lang")
    public String changeLanguage(@RequestParam(name = "lang", defaultValue = "es") String lang,
                                 HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        // El interceptor ya cambio el locale; volvemos a la pagina de origen
        return "redirect:" + (referer != null ? referer : "/");
    }
}
