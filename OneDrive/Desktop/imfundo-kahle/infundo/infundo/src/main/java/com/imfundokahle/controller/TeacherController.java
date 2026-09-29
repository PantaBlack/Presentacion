package com.imfundokahle.controller;

import com.imfundokahle.model.*;
import com.imfundokahle.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador del portal de profesor (/teacher/**).
 */
@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final ProposalService proposalService;
    private final MaterialService materialService;
    private final ActividadService actividadService;
    private final MaterialFileValidator materialFileValidator;
    private final AsistenciaService asistenciaService;
    private final EstadoSemanalService estadoSemanalService;

    public TeacherController(UserService userService, ScheduleService scheduleService,
                            ProposalService proposalService, MaterialService materialService,
                            ActividadService actividadService, MaterialFileValidator materialFileValidator,
                            AsistenciaService asistenciaService, EstadoSemanalService estadoSemanalService) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.proposalService = proposalService;
        this.materialService = materialService;
        this.actividadService = actividadService;
        this.materialFileValidator = materialFileValidator;
        this.asistenciaService = asistenciaService;
        this.estadoSemanalService = estadoSemanalService;
    }

    /** Obtiene el profesor autenticado. */
    private User current(Principal principal) {
        return userService.findByEmail(principal.getName()).orElseThrow();
    }

    /** Dashboard personal del profesor. */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        construirModeloDashboard(current(principal), model);
        return "teacher/dashboard";
    }

    /**
     * Arma el modelo del panel de un profesor cualquiera. Separado de {@link #dashboard}
     * para que el admin pueda previsualizar y editar esta misma pantalla (con un profesor
     * real cualquiera, no datos inventados) desde /admin/contenido/editor/profesor, sin
     * duplicar esta logica en dos lugares que se puedan desincronizar con el tiempo.
     */
    void construirModeloDashboard(User teacher, Model model) {
        model.addAttribute("mySchedules", scheduleService.countByTeacher(teacher));
        model.addAttribute("pendingProposals",
                proposalService.countByTeacherAndStatus(teacher, ProposalStatus.PENDING));
        model.addAttribute("myMaterials", materialService.countByTeacher(teacher));
        model.addAttribute("myStudents", scheduleService.countStudentsOfTeacher(teacher));

        List<Schedule> mine = scheduleService.findByTeacher(teacher);
        model.addAttribute("schedules", mine);
        model.addAttribute("recentProposals", proposalService.findByTeacher(teacher));
        model.addAttribute("approvedProposals",
                proposalService.countByTeacherAndStatus(teacher, ProposalStatus.APPROVED));
        model.addAttribute("rejectedProposals",
                proposalService.countByTeacherAndStatus(teacher, ProposalStatus.REJECTED));

        // Ocupacion de las clases del profesor (plazas ocupadas / ofertadas)
        long seatsTotal = 0;
        long seatsTaken = 0;
        for (Schedule s : mine) {
            seatsTotal += s.getMaxStudents();
            seatsTaken += scheduleService.countActiveEnrollments(s);
        }
        model.addAttribute("seatsTotal", seatsTotal);
        model.addAttribute("seatsTaken", seatsTaken);
        model.addAttribute("scheduleService", scheduleService);

        // Agenda semanal ya agrupada por dia (evita filtrar dentro de la vista)
        Map<DayOfWeekEnum, List<Schedule>> byDay = new LinkedHashMap<>();
        for (DayOfWeekEnum d : DayOfWeekEnum.values()) {
            byDay.put(d, new ArrayList<>());
        }
        for (Schedule s : mine) {
            if (s.getDayOfWeek() != null) {
                byDay.get(s.getDayOfWeek()).add(s);
            }
        }
        model.addAttribute("schedulesByDay", byDay);

        // Tareas que el administrador le haya asignado (panel de Actividades y Horas)
        model.addAttribute("misActividades", actividadService.obtenerResumenPara(teacher.getId()));
    }

    /** Gestion de horarios: vista semanal. */
    @GetMapping("/schedules")
    public String schedules(Principal principal, Model model) {
        User teacher = current(principal);
        model.addAttribute("schedules", scheduleService.findByTeacher(teacher));
        model.addAttribute("scheduleService", scheduleService);
        model.addAttribute("days", DayOfWeekEnum.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("newSchedule", new Schedule());
        LocalDate semanaActual = estadoSemanalService.inicioDeSemanaActual();
        model.addAttribute("semanaActual", semanaActual);
        model.addAttribute("canceladasEstaSemana", estadoSemanalService.canceladasEnSemana(semanaActual));
        return "teacher/schedules";
    }

    /** Marca una clase propia como cancelada esta semana (el motivo es obligatorio). */
    @PostMapping("/schedules/{id}/cancelar-semana")
    public String cancelarSemana(Principal principal, @PathVariable Long id,
                                 @RequestParam String motivo, RedirectAttributes ra) {
        Schedule s = scheduleService.findById(id);
        boolean ok = estadoSemanalService.cancelarSemana(s, current(principal),
                estadoSemanalService.inicioDeSemanaActual(), motivo);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "semana.cancelada" : "semana.motivoRequerido");
        return "redirect:/teacher/schedules";
    }

    /** Vuelve a marcar una clase propia como activa esta semana. */
    @PostMapping("/schedules/{id}/reactivar-semana")
    public String reactivarSemana(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        Schedule s = scheduleService.findById(id);
        boolean ok = estadoSemanalService.reactivarSemana(s, current(principal), estadoSemanalService.inicioDeSemanaActual());
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "semana.reactivada" : "schedule.invalido");
        return "redirect:/teacher/schedules";
    }

    /** Crear un nuevo horario. */
    @PostMapping("/schedules")
    public String createSchedule(Principal principal,
                                 @RequestParam String dayOfWeek,
                                 @RequestParam String startTime,
                                 @RequestParam String endTime,
                                 @RequestParam String subject,
                                 @RequestParam String language,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(defaultValue = "10") int maxStudents,
                                 @RequestParam(required = false) String meetLink,
                                 RedirectAttributes ra) {
        Schedule s = new Schedule();
        try {
            s.setDayOfWeek(DayOfWeekEnum.valueOf(dayOfWeek));
            s.setStartTime(LocalTime.parse(startTime));
            s.setEndTime(LocalTime.parse(endTime));
            s.setLanguage(Language.valueOf(language));
        } catch (RuntimeException e) {
            // Valores fuera de los que ofrece el formulario (dia/idioma/hora manipulados
            // a mano, por ejemplo desde el inspector del navegador): se rechaza en vez de
            // dejar pasar cualquier texto o tirar un 500.
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/teacher/schedules";
        }
        if (maxStudents < 1) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/teacher/schedules";
        }
        if (meetLink != null && !meetLink.isBlank() && !meetLink.matches("(?i)^https?://.+")) {
            ra.addFlashAttribute("errorKey", "schedule.meetLink.invalido");
            return "redirect:/teacher/schedules";
        }
        s.setTeacher(current(principal));
        s.setSubject(subject);
        s.setDescription(description);
        s.setMaxStudents(maxStudents);
        s.setMeetLink(meetLink != null && !meetLink.isBlank() ? meetLink : null);
        scheduleService.save(s);
        ra.addFlashAttribute("successKey", "schedule.created");
        return "redirect:/teacher/schedules";
    }

    /** Edita dia/hora/materia/idioma/descripcion/cupo de un horario propio, sin perder las inscripciones. */
    @PostMapping("/schedules/{id}/editar")
    public String editarSchedule(Principal principal, @PathVariable Long id,
                                 @RequestParam String dayOfWeek,
                                 @RequestParam String startTime,
                                 @RequestParam String endTime,
                                 @RequestParam String subject,
                                 @RequestParam String language,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(defaultValue = "10") int maxStudents,
                                 RedirectAttributes ra) {
        User teacher = current(principal);
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
            return "redirect:/teacher/schedules";
        }
        if (maxStudents < 1) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/teacher/schedules";
        }
        boolean ok = scheduleService.actualizar(id, teacher, dia, inicio, fin, subject, idioma, description, maxStudents);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "schedule.actualizado" : "schedule.cupoMenor");
        return "redirect:/teacher/schedules";
    }

    /** Agregar o cambiar el enlace de videollamada de un horario ya creado. */
    @PostMapping("/schedules/{id}/meet-link")
    public String updateMeetLink(Principal principal, @PathVariable Long id,
                                 @RequestParam(required = false) String meetLink, RedirectAttributes ra) {
        String limpio = meetLink != null && !meetLink.isBlank() ? meetLink.trim() : null;
        if (limpio != null && !limpio.matches("(?i)^https?://.+")) {
            ra.addFlashAttribute("errorKey", "schedule.meetLink.invalido");
            return "redirect:/teacher/schedules";
        }
        boolean ok = scheduleService.updateMeetLink(id, current(principal), limpio);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey", ok ? "schedule.meetLink.saved" : "schedule.invalido");
        return "redirect:/teacher/schedules";
    }

    /** Eliminar un horario propio. */
    @PostMapping("/schedules/{id}/delete")
    public String deleteSchedule(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        Schedule s = scheduleService.findById(id);
        if (s != null && s.getTeacher().getEmail().equals(principal.getName())) {
            scheduleService.delete(id);
            ra.addFlashAttribute("successKey", "schedule.deleted");
        }
        return "redirect:/teacher/schedules";
    }

    /** Ver alumnos inscritos en un horario, con asistencia del dia y notas de progreso. */
    @GetMapping("/schedules/{id}/students")
    public String scheduleStudents(Principal principal, @PathVariable Long id, Model model) {
        Schedule s = scheduleService.findById(id);
        if (s == null || !s.getTeacher().getEmail().equals(principal.getName())) {
            return "redirect:/teacher/schedules";
        }
        List<ClassEnrollment> enrollments = scheduleService.findEnrollmentsBySchedule(s);
        model.addAttribute("schedule", s);
        model.addAttribute("enrollments", enrollments);
        LocalDate hoy = LocalDate.now();
        model.addAttribute("hoy", hoy);
        List<User> alumnos = enrollments.stream().map(ClassEnrollment::getStudent).collect(Collectors.toList());
        model.addAttribute("asistenciaHoy", asistenciaService.asistenciaDelDia(s, hoy, alumnos));
        return "teacher/schedule-students";
    }

    /** Guarda la asistencia de una fecha y las notas de progreso de todos los inscritos, de una sola vez. */
    @PostMapping("/schedules/{id}/asistencia")
    public String guardarAsistencia(Principal principal, @PathVariable Long id,
                                    @RequestParam String fecha,
                                    @RequestParam Map<String, String> params, RedirectAttributes ra) {
        Schedule s = scheduleService.findById(id);
        if (s == null || !s.getTeacher().getEmail().equals(principal.getName())) {
            return "redirect:/teacher/schedules";
        }
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorKey", "schedule.invalido");
            return "redirect:/teacher/schedules/" + id + "/students";
        }
        User teacher = current(principal);
        for (ClassEnrollment en : scheduleService.findEnrollmentsBySchedule(s)) {
            boolean presente = params.containsKey("presente_" + en.getId());
            asistenciaService.marcar(s, en.getStudent(), dia, presente);
            scheduleService.guardarNota(en.getId(), teacher, params.get("notas_" + en.getId()));
        }
        ra.addFlashAttribute("successKey", "asistencia.guardada");
        return "redirect:/teacher/schedules/" + id + "/students";
    }

    /** Lista y creacion de propuestas. */
    @GetMapping("/proposals")
    public String proposals(Principal principal, Model model) {
        User teacher = current(principal);
        model.addAttribute("proposals", proposalService.findByTeacher(teacher));
        model.addAttribute("types", ProposalType.values());
        return "teacher/proposals";
    }

    @PostMapping("/proposals")
    public String createProposal(Principal principal,
                                 @RequestParam String type,
                                 @RequestParam String title,
                                 @RequestParam String description,
                                 RedirectAttributes ra) {
        ProposalType tipo;
        try {
            tipo = ProposalType.valueOf(type);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorKey", "proposal.invalido");
            return "redirect:/teacher/proposals";
        }
        User teacher = current(principal);
        Proposal p = new Proposal();
        p.setTeacher(teacher);
        p.setType(tipo);
        p.setTitle(title);
        p.setDescription(description);
        p.setStatus(ProposalStatus.PENDING);
        proposalService.save(p);
        ra.addFlashAttribute("successKey", "proposal.created");
        return "redirect:/teacher/proposals";
    }

    /** Gestion de materiales. */
    @GetMapping("/materials")
    public String materials(Principal principal, Model model) {
        User teacher = current(principal);
        model.addAttribute("materials", materialService.findByTeacher(teacher));
        model.addAttribute("categories", MaterialCategory.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("visibilities", MaterialVisibility.values());
        model.addAttribute("schedules", scheduleService.findByTeacher(teacher));
        return "teacher/materials";
    }

    @PostMapping("/materials")
    public String createMaterial(Principal principal,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam String category,
                                 @RequestParam String language,
                                 @RequestParam(required = false) String externalLink,
                                 @RequestParam(defaultValue = "EVERYONE") String visibility,
                                 @RequestParam(required = false) String scheduleId,
                                 @RequestParam(required = false) MultipartFile archivo,
                                 RedirectAttributes ra) {
        User teacher = current(principal);

        // Si eligio una clase, debe ser una clase suya (evita asignar el material a la
        // clase de otro profesor manipulando el formulario a mano). "scheduleId" viaja
        // como String (no Long) porque el <select> manda "" para la opcion "General",
        // y Spring no convierte "" a null automaticamente para tipos numericos.
        Schedule scheduleElegido = null;
        if (scheduleId != null && !scheduleId.isBlank()) {
            try {
                scheduleElegido = scheduleService.findById(Long.valueOf(scheduleId));
            } catch (NumberFormatException e) {
                ra.addFlashAttribute("errorKey", "material.invalido");
                return "redirect:/teacher/materials";
            }
            if (scheduleElegido == null || !scheduleElegido.getTeacher().getId().equals(teacher.getId())) {
                ra.addFlashAttribute("errorKey", "material.invalido");
                return "redirect:/teacher/materials";
            }
        }

        boolean hayArchivo = archivo != null && !archivo.isEmpty();
        boolean hayEnlace = externalLink != null && !externalLink.isBlank();
        if (!hayArchivo && !hayEnlace) {
            ra.addFlashAttribute("errorKey", "material.needsFileOrLink");
            return "redirect:/teacher/materials";
        }
        // Solo http(s): sin esto, un enlace "javascript:..." o "data:..." se guardaria
        // tal cual y se renderizaria como href para cualquiera que abra el material
        // (XSS almacenado ejecutandose en el navegador de quien le de clic).
        if (hayEnlace && !externalLink.matches("(?i)^https?://.+")) {
            ra.addFlashAttribute("errorKey", "material.invalido");
            return "redirect:/teacher/materials";
        }

        Material m = new Material();
        try {
            m.setCategory(MaterialCategory.valueOf(category));
            m.setLanguage(Language.valueOf(language));
            m.setVisibility(MaterialVisibility.valueOf(visibility));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorKey", "material.invalido");
            return "redirect:/teacher/materials";
        }
        m.setTeacher(teacher);
        m.setTitle(title);
        m.setDescription(description);
        m.setExternalLink(hayEnlace ? externalLink : null);
        m.setSchedule(scheduleElegido);

        if (hayArchivo) {
            byte[] bytes;
            try {
                bytes = archivo.getBytes();
            } catch (IOException e) {
                ra.addFlashAttribute("errorKey", "material.uploadError");
                return "redirect:/teacher/materials";
            }

            // Tamano: defensa en profundidad ademas del limite global de Spring/Tomcat
            // (spring.servlet.multipart.max-file-size), por si ese limite cambia algun dia.
            if (!materialFileValidator.validarTamano(bytes).isValido()) {
                ra.addFlashAttribute("errorKey", "material.fileTooLarge");
                return "redirect:/teacher/materials";
            }

            // Tipo real por contenido (magic bytes), nunca por la extension del nombre
            // ni por el Content-Type que declara el navegador: ambos los controla quien
            // sube el archivo. Un .exe o .php renombrado como "apunte.pdf" se rechaza aqui.
            MaterialFileValidator.Resultado tipo = materialFileValidator.validarTipo(bytes);
            if (!tipo.isValido()) {
                ra.addFlashAttribute("errorKey", "material.invalidFileType");
                return "redirect:/teacher/materials";
            }

            m.setFileName(sanitizarNombreArchivo(archivo.getOriginalFilename()));
            // Content-Type que se enviara al descargar: el que detecto el servidor a
            // partir del contenido real, nunca el que declaro el navegador al subirlo.
            m.setFileType(tipo.getTipoDetectado());
            m.setFileSize((long) bytes.length);
            m.setFileData(bytes);
        }

        materialService.save(m);
        ra.addFlashAttribute("successKey", "material.created");
        return "redirect:/teacher/materials";
    }

    /**
     * Se queda solo con el nombre de archivo (algunos navegadores envian la ruta
     * completa), quita caracteres de control y lo acota a un largo razonable, para
     * que lo que se guarda y luego se refleja en la cabecera de descarga sea siempre
     * texto plano y corto, nunca una ruta ni contenido capaz de manipular esa cabecera.
     */
    private String sanitizarNombreArchivo(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "archivo";
        }
        String soloNombre = nombreOriginal.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);
        soloNombre = soloNombre.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
        if (soloNombre.isEmpty()) {
            soloNombre = "archivo";
        }
        return soloNombre.length() > 255 ? soloNombre.substring(0, 255) : soloNombre;
    }

    /** El archivo supera el limite configurado (spring.servlet.multipart.max-file-size). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande(RedirectAttributes ra) {
        ra.addFlashAttribute("errorKey", "material.fileTooLarge");
        return "redirect:/teacher/materials";
    }

    @PostMapping("/materials/{id}/delete")
    public String deleteMaterial(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        Material m = materialService.findById(id);
        if (m != null && m.getTeacher().getEmail().equals(principal.getName())) {
            materialService.delete(id);
            ra.addFlashAttribute("successKey", "material.deleted");
        }
        return "redirect:/teacher/materials";
    }
}
