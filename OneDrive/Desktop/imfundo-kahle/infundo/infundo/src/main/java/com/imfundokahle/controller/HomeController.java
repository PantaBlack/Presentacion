package com.imfundokahle.controller;

import com.imfundokahle.model.Role;
import com.imfundokahle.service.ContentImageService;
import com.imfundokahle.service.CustomSectionService;
import com.imfundokahle.service.MaterialService;
import com.imfundokahle.service.PromoBannerService;
import com.imfundokahle.service.ScheduleService;
import com.imfundokahle.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la pagina publica de inicio (landing page).
 */
@Controller
public class HomeController {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final MaterialService materialService;
    private final PromoBannerService promoBannerService;
    private final ContentImageService contentImageService;
    private final CustomSectionService customSectionService;

    public HomeController(UserService userService, ScheduleService scheduleService,
                          MaterialService materialService, PromoBannerService promoBannerService,
                          ContentImageService contentImageService, CustomSectionService customSectionService) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.materialService = materialService;
        this.promoBannerService = promoBannerService;
        this.contentImageService = contentImageService;
        this.customSectionService = customSectionService;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        // Estadisticas publicas animadas
        model.addAttribute("totalTeachers", userService.countByRole(Role.TEACHER));
        model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
        model.addAttribute("totalClasses", scheduleService.countActiveSchedules());
        model.addAttribute("totalMaterials", materialService.countAll());
        model.addAttribute("promoTema", promoBannerService.getTema().name());
        // "?v=" en cada imagen editable: evita que el navegador se quede mostrando
        // una version vieja en cache despues de que el admin suba un reemplazo.
        model.addAttribute("imgV", contentImageService.getVersiones());
        // Bloques de texto que el admin agrego desde "Contenido del sitio" (ademas
        // de los campos fijos del diseno): solo los que no dejo ocultos.
        model.addAttribute("seccionesPersonalizadas", customSectionService.listarVisibles());
        return "index";
    }
}
