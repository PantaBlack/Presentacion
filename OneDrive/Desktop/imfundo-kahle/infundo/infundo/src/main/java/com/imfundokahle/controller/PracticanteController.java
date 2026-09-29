package com.imfundokahle.controller;

import com.imfundokahle.model.*;
import com.imfundokahle.service.ActividadResumen;
import com.imfundokahle.service.ActividadService;
import com.imfundokahle.service.AsistenciaService;
import com.imfundokahle.service.EstadoSemanalService;
import com.imfundokahle.service.ScheduleService;
import com.imfundokahle.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador del portal de practicante (/practicante/**).
 * Muestra las actividades que el administrador le ha asignado, el resumen de
 * horas planificadas, y sus clases personalizadas (para los alumnos especificos
 * que el practicante elija, sin tope fijo de cantidad).
 */
@Controller
@RequestMapping("/practicante")
public class PracticanteController {

    private final ActividadService actividadService;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final AsistenciaService asistenciaService;
    private final EstadoSemanalService estadoSemanalService;

    public PracticanteController(ActividadService actividadService, UserService userService,
                                 ScheduleService scheduleService, AsistenciaService asistenciaService,
                                 EstadoSemanalService estadoSemanalService) {
        this.actividadService = actividadService;
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.asistenciaService = asistenciaService;
        this.estadoSemanalService = estadoSemanalService;
    }

    private User current(Principal principal) {
        return userService.findByEmail(principal.getName()).orElseThrow();
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        construirModeloDashboard(current(principal), model);
        return "practicante/dashboard";
    }

    /**
     * Arma el modelo del panel de un practicante cualquiera. Separado de {@link #dashboard}
     * para que el admin pueda previsualizar y editar esta misma pantalla (con un practicante
     * real cualquiera) desde /admin/contenido/editor/practicante, sin duplicar esta logica.
     */
    void construirModeloDashboard(User user, Model model) {
        model.addAttribute("title", "Panel de Practicante");
        model.addAttribute("activeTab", "dashboard");

        ActividadResumen resumen = actividadService.obtenerResumenPara(user.getId());

        model.addAttribute("actividades", resumen.actividades());
        model.addAttribute("totalActividades", resumen.total());
        model.addAttribute("pendientes", resumen.pendientes());
        model.addAttribute("enCurso", resumen.enCurso());
        model.addAttribute("completadas", resumen.completadas());
        model.addAttribute("horasPlanificadas", resumen.horasPlanificadas());
        model.addAttribute("horasCompletadas", resumen.horasCompletadas());
    }

    /**
     * Clases personalizadas propias: a diferencia de las clases grupales de idiomas,
     * estas no se ofrecen en un listado abierto para que cualquier alumno se
     * autoinscriba. El practicante elige directamente a los alumnos al crearla
     * (uno, varios, sin limite fijo).
     */
    @GetMapping("/clases")
    public String clases(Principal principal, Model model) {
        User practicante = current(principal);
        model.addAttribute("clases", scheduleService.findByTeacher(practicante));
        model.addAttribute("scheduleService", scheduleService);
        model.addAttribute("alumnos", userService.findByRole(Role.STUDENT));
        model.addAttribute("languages", Language.values());
        model.addAttribute("days", DayOfWeekEnum.values());
        model.addAttribute("activeTab", "clases");
        LocalDate semanaActual = estadoSemanalService.inicioDeSemanaActual();
        model.addAttribute("semanaActual", semanaActual);
        model.addAttribute("canceladasEstaSemana", estadoSemanalService.canceladasEnSemana(semanaActual));
        return "practicante/clases";
    }

    /** Marca una clase propia como cancelada esta semana (el motivo es obligatorio). */
    @PostMapping("/clases/{id}/cancelar-semana")
    public String cancelarSemana(Principal principal, @PathVariable Long id,
                                 @RequestParam String motivo, RedirectAttributes ra) {
        Schedule s = scheduleService.findById(id);
        boolean ok = estadoSemanalService.cancelarSemana(s, current(principal),
                estadoSemanalService.inicioDeSemanaActual(), motivo);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "semana.cancelada" : "semana.motivoRequerido");
        return "redirect:/practicante/clases";
    }

    /** Vuelve a marcar una clase propia como activa esta semana. */
    @PostMapping("/clases/{id}/reactivar-semana")
    public String reactivarSemana(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        Schedule s = scheduleService.findById(id);
        boolean ok = estadoSemanalService.reactivarSemana(s, current(principal), estadoSemanalService.inicioDeSemanaActual());
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "semana.reactivada" : "schedule.invalido");
        return "redirect:/practicante/clases";
    }

    @PostMapping("/clases")
    public String crearClase(Principal principal,
                             @RequestParam String dayOfWeek,
                             @RequestParam String startTime,
                             @RequestParam String endTime,
                             @RequestParam String subject,
                             @RequestParam String language,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) List<Long> alumnoIds,
                             @RequestParam(required = false) String meetLink,
                             RedirectAttributes ra) {
        User practicante = current(principal);

        // Sin tope fijo: puede ser 1, 2 o los que hagan falta. Solo se exige que
        // haya elegido al menos uno, que existan de verdad y que sean alumnos.
        if (alumnoIds == null || alumnoIds.isEmpty()) {
            ra.addFlashAttribute("errorKey", "practicante.clase.invalida");
            return "redirect:/practicante/clases";
        }
        List<User> alumnos = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(alumnoIds)) {
            User a = userService.findById(id).orElse(null);
            if (a == null || a.getRole() != Role.STUDENT) {
                ra.addFlashAttribute("errorKey", "practicante.clase.invalida");
                return "redirect:/practicante/clases";
            }
            alumnos.add(a);
        }

        Schedule s = new Schedule();
        try {
            s.setDayOfWeek(DayOfWeekEnum.valueOf(dayOfWeek));
            s.setStartTime(LocalTime.parse(startTime));
            s.setEndTime(LocalTime.parse(endTime));
            s.setLanguage(Language.valueOf(language));
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/practicante/clases";
        }
        if (meetLink != null && !meetLink.isBlank() && !meetLink.matches("(?i)^https?://.+")) {
            ra.addFlashAttribute("errorKey", "schedule.meetLink.invalido");
            return "redirect:/practicante/clases";
        }

        s.setTeacher(practicante);
        s.setSubject(subject);
        s.setDescription(description);
        s.setPersonalizada(true);
        s.setMaxStudents(alumnos.size());
        s.setMeetLink(meetLink != null && !meetLink.isBlank() ? meetLink : null);
        scheduleService.save(s);

        for (User alumno : alumnos) {
            scheduleService.asignarAlumno(alumno, s);
        }

        ra.addFlashAttribute("successKey", "practicante.clase.creada");
        return "redirect:/practicante/clases";
    }

    /** Edita dia/hora/materia/idioma/descripcion de una clase propia sin perder las inscripciones. */
    @PostMapping("/clases/{id}/editar")
    public String editarClase(Principal principal, @PathVariable Long id,
                              @RequestParam String dayOfWeek,
                              @RequestParam String startTime,
                              @RequestParam String endTime,
                              @RequestParam String subject,
                              @RequestParam String language,
                              @RequestParam(required = false) String description,
                              RedirectAttributes ra) {
        User practicante = current(principal);
        Schedule actual = scheduleService.findById(id);
        if (actual == null || !actual.getTeacher().getId().equals(practicante.getId())) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/practicante/clases";
        }
        DayOfWeekEnum dia;
        LocalTime inicio;
        LocalTime fin;
        Language idioma;
        try {
            dia = DayOfWeekEnum.valueOf(dayOfWeek);
            inicio = LocalTime.parse(startTime);
            fin = LocalTime.parse(endTime);
            idioma = Language.valueOf(language);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/practicante/clases";
        }
        boolean ok = scheduleService.actualizar(id, practicante, dia, inicio, fin, subject, idioma,
                description, actual.getMaxStudents());
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "schedule.actualizado" : "schedule.invalido");
        return "redirect:/practicante/clases";
    }

    /** Agrega un alumno mas a una clase personalizada ya creada (sin tope: si hace falta, agranda el cupo). */
    @PostMapping("/clases/{id}/agregar-alumno")
    public String agregarAlumno(Principal principal, @PathVariable Long id,
                                @RequestParam Long alumnoId, RedirectAttributes ra) {
        User practicante = current(principal);
        Schedule s = scheduleService.findById(id);
        if (s == null || !s.getTeacher().getId().equals(practicante.getId())) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/practicante/clases";
        }
        User alumno = userService.findById(alumnoId).orElse(null);
        if (alumno == null || alumno.getRole() != Role.STUDENT) {
            ra.addFlashAttribute("errorKey", "practicante.clase.invalida");
            return "redirect:/practicante/clases";
        }
        long activos = scheduleService.countActiveEnrollments(s);
        if (activos >= s.getMaxStudents()) {
            // Sin tope: si ya estaba al cupo, se agranda justo lo necesario para sumar a este alumno.
            s.setMaxStudents((int) activos + 1);
            scheduleService.save(s);
        }
        String resultado = scheduleService.asignarAlumno(alumno, s);
        if ("already".equals(resultado)) {
            ra.addFlashAttribute("errorKey", "practicante.clase.yaInscrito");
        } else {
            ra.addFlashAttribute("successKey", "practicante.clase.alumnoAgregado");
        }
        return "redirect:/practicante/clases";
    }

    /** Ver alumnos de una clase propia, con asistencia del dia y notas de progreso. */
    @GetMapping("/clases/{id}/alumnos")
    public String claseAlumnos(Principal principal, @PathVariable Long id, Model model) {
        User practicante = current(principal);
        Schedule s = scheduleService.findById(id);
        if (s == null || !s.getTeacher().getId().equals(practicante.getId())) {
            return "redirect:/practicante/clases";
        }
        List<ClassEnrollment> enrollments = scheduleService.findEnrollmentsBySchedule(s);
        model.addAttribute("schedule", s);
        model.addAttribute("enrollments", enrollments);
        LocalDate hoy = LocalDate.now();
        model.addAttribute("hoy", hoy);
        List<User> alumnos = enrollments.stream().map(ClassEnrollment::getStudent).collect(Collectors.toList());
        model.addAttribute("asistenciaHoy", asistenciaService.asistenciaDelDia(s, hoy, alumnos));
        model.addAttribute("activeTab", "clases");
        return "practicante/clase-alumnos";
    }

    /** Guarda la asistencia de una fecha y las notas de progreso de todos los inscritos, de una sola vez. */
    @PostMapping("/clases/{id}/asistencia")
    public String guardarAsistencia(Principal principal, @PathVariable Long id,
                                    @RequestParam String fecha,
                                    @RequestParam Map<String, String> params, RedirectAttributes ra) {
        User practicante = current(principal);
        Schedule s = scheduleService.findById(id);
        if (s == null || !s.getTeacher().getId().equals(practicante.getId())) {
            return "redirect:/practicante/clases";
        }
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/practicante/clases/" + id + "/alumnos";
        }
        for (ClassEnrollment en : scheduleService.findEnrollmentsBySchedule(s)) {
            boolean presente = params.containsKey("presente_" + en.getId());
            asistenciaService.marcar(s, en.getStudent(), dia, presente);
            scheduleService.guardarNota(en.getId(), practicante, params.get("notas_" + en.getId()));
        }
        ra.addFlashAttribute("successKey", "asistencia.guardada");
        return "redirect:/practicante/clases/" + id + "/alumnos";
    }

    /** Agregar o cambiar el enlace de videollamada de una clase personalizada propia. */
    @PostMapping("/clases/{id}/meet-link")
    public String actualizarEnlace(Principal principal, @PathVariable Long id,
                                   @RequestParam(required = false) String meetLink, RedirectAttributes ra) {
        String limpio = meetLink != null && !meetLink.isBlank() ? meetLink.trim() : null;
        if (limpio != null && !limpio.matches("(?i)^https?://.+")) {
            ra.addFlashAttribute("errorKey", "schedule.meetLink.invalido");
            return "redirect:/practicante/clases";
        }
        boolean ok = scheduleService.updateMeetLink(id, current(principal), limpio);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "schedule.meetLink.saved" : "schedule.invalido");
        return "redirect:/practicante/clases";
    }

    @PostMapping("/clases/{id}/delete")
    public String eliminarClase(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        User practicante = current(principal);
        Schedule s = scheduleService.findById(id);
        if (s != null && s.getTeacher().getId().equals(practicante.getId())) {
            scheduleService.delete(id);
            ra.addFlashAttribute("successKey", "schedule.deleted");
        }
        return "redirect:/practicante/clases";
    }
}
