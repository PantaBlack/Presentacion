package com.imfundokahle.controller;

import com.imfundokahle.model.*;
import com.imfundokahle.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador del portal de alumno (/student/**).
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    /** Mismo patron que RegisterForm: solo letras, espacios, guiones y apostrofes. */
    private static final java.util.regex.Pattern NAME_PATTERN =
            java.util.regex.Pattern.compile("^[\\p{L} '-]+$");
    private static final java.util.Set<String> IDIOMAS_VALIDOS = java.util.Set.of("es", "fr", "en");

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final MaterialService materialService;
    private final ActividadService actividadService;
    private final EstadoSemanalService estadoSemanalService;

    public StudentController(UserService userService, ScheduleService scheduleService,
                            MaterialService materialService, ActividadService actividadService,
                            EstadoSemanalService estadoSemanalService) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.materialService = materialService;
        this.actividadService = actividadService;
        this.estadoSemanalService = estadoSemanalService;
    }

    private User current(Principal principal) {
        return userService.findByEmail(principal.getName()).orElseThrow();
    }

    /** Dashboard del alumno. */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        construirModeloDashboard(current(principal), model);
        return "student/dashboard";
    }

    /**
     * Arma el modelo del panel de un alumno cualquiera. Separado de {@link #dashboard}
     * para que el admin pueda previsualizar y editar esta misma pantalla (con un alumno
     * real cualquiera) desde /admin/contenido/editor/alumno, sin duplicar esta logica.
     */
    void construirModeloDashboard(User student, Model model) {
        List<ClassEnrollment> enrollments = scheduleService.findEnrollmentsByStudent(student);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("enrolledCount", enrollments.size());
        model.addAttribute("materials", materialService.findVisibleToStudent(student).stream().limit(6).collect(Collectors.toList()));
        model.addAttribute("totalMaterials", materialService.countAll());

        // Minutos de clase a la semana, para mostrar la carga real del alumno
        long weeklyMinutes = enrollments.stream()
                .mapToLong(e -> java.time.Duration.between(
                        e.getSchedule().getStartTime(), e.getSchedule().getEndTime()).toMinutes())
                .filter(m -> m > 0)
                .sum();
        model.addAttribute("weeklyMinutes", weeklyMinutes);

        // Clases sugeridas: las que aun tienen cupo y en las que no esta inscrito
        List<Schedule> suggested = scheduleService.findOpenForEnrollment().stream()
                .filter(s -> !scheduleService.isStudentEnrolled(student, s))
                .filter(s -> scheduleService.availableSeats(s) > 0)
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("suggestedClasses", suggested);
        model.addAttribute("totalClasses", scheduleService.countActiveSchedules());
        model.addAttribute("scheduleService", scheduleService);

        // Agenda semanal del alumno, agrupada por dia
        Map<DayOfWeekEnum, List<Schedule>> byDay = new LinkedHashMap<>();
        for (DayOfWeekEnum d : DayOfWeekEnum.values()) {
            byDay.put(d, new ArrayList<>());
        }
        for (ClassEnrollment e : enrollments) {
            Schedule s = e.getSchedule();
            if (s != null && s.getDayOfWeek() != null) {
                byDay.get(s.getDayOfWeek()).add(s);
            }
        }
        model.addAttribute("schedulesByDay", byDay);

        // Tareas que el administrador le haya asignado (panel de Actividades y Horas)
        model.addAttribute("misActividades", actividadService.obtenerResumenPara(student.getId()));
    }

    /** Ver todas las clases disponibles y las inscritas. */
    @GetMapping("/classes")
    public String classes(Principal principal,
                          @RequestParam(required = false) String language,
                          Model model) {
        User student = current(principal);
        List<Schedule> schedules = scheduleService.findOpenForEnrollment();
        if (language != null && !language.isEmpty()) {
            try {
                Language lang = Language.valueOf(language);
                schedules = schedules.stream().filter(s -> s.getLanguage() == lang).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        model.addAttribute("schedules", schedules);
        model.addAttribute("scheduleService", scheduleService);
        model.addAttribute("student", student);
        model.addAttribute("myEnrollments", scheduleService.findEnrollmentsByStudent(student));
        model.addAttribute("languages", Language.values());
        model.addAttribute("languageFilter", language);
        model.addAttribute("canceladasEstaSemana", estadoSemanalService.canceladasEnSemana(estadoSemanalService.inicioDeSemanaActual()));
        return "student/classes";
    }

    /** Inscribirse en una clase. */
    @PostMapping("/classes/{id}/enroll")
    public String enroll(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        User student = current(principal);
        Schedule s = scheduleService.findById(id);
        if (s != null) {
            String result = scheduleService.enroll(student, s);
            switch (result) {
                case "ok": ra.addFlashAttribute("successKey", "enroll.success"); break;
                case "full": ra.addFlashAttribute("errorKey", "enroll.full"); break;
                case "already": ra.addFlashAttribute("errorKey", "enroll.already"); break;
                default: break;
            }
        }
        return "redirect:/student/classes";
    }

    /** Cancelar inscripcion. */
    @PostMapping("/classes/{id}/cancel")
    public String cancel(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        User student = current(principal);
        Schedule s = scheduleService.findById(id);
        if (s != null) {
            scheduleService.cancelEnrollment(student, s);
            ra.addFlashAttribute("successKey", "enroll.cancelled");
        }
        return "redirect:/student/classes";
    }

    /** Materiales disponibles con filtros. */
    @GetMapping("/materials")
    public String materials(Principal principal,
                            @RequestParam(required = false) String language,
                            @RequestParam(required = false) String category,
                            Model model) {
        List<Material> materials = materialService.findVisibleToStudent(current(principal));
        if (language != null && !language.isEmpty()) {
            try {
                Language lang = Language.valueOf(language);
                materials = materials.stream().filter(m -> m.getLanguage() == lang).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        if (category != null && !category.isEmpty()) {
            try {
                MaterialCategory cat = MaterialCategory.valueOf(category);
                materials = materials.stream().filter(m -> m.getCategory() == cat).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        model.addAttribute("materials", materials);
        model.addAttribute("languages", Language.values());
        model.addAttribute("categories", MaterialCategory.values());
        model.addAttribute("languageFilter", language);
        model.addAttribute("categoryFilter", category);
        return "student/materials";
    }

    /** Ver y editar perfil. */
    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("user", current(principal));
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Principal principal,
                                @RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String preferredLanguage,
                                RedirectAttributes ra) {
        firstName = firstName == null ? "" : firstName.trim();
        lastName = lastName == null ? "" : lastName.trim();
        // Mismas reglas que el registro: si alguien manipula el formulario (o lo envia
        // directo con el inspector) para meter numeros, simbolos o un idioma que no
        // existe en el select, se rechaza en vez de guardarlo.
        boolean nombreValido = !firstName.isEmpty() && firstName.length() <= 80 && NAME_PATTERN.matcher(firstName).matches();
        boolean apellidoValido = !lastName.isEmpty() && lastName.length() <= 80 && NAME_PATTERN.matcher(lastName).matches();
        if (!nombreValido || !apellidoValido || !IDIOMAS_VALIDOS.contains(preferredLanguage)) {
            ra.addFlashAttribute("errorKey", "validation.name.invalid");
            return "redirect:/student/profile";
        }
        User student = current(principal);
        userService.updateProfile(student, firstName, lastName, preferredLanguage);
        ra.addFlashAttribute("successKey", "profile.updated");
        return "redirect:/student/profile";
    }
}
