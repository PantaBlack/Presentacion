package com.imfundokahle.controller;

import com.imfundokahle.model.*;
import com.imfundokahle.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador del portal de administrador (/admin/**).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final ProposalService proposalService;
    private final MaterialService materialService;
    private final EstadoSemanalService estadoSemanalService;

    public AdminController(UserService userService, ScheduleService scheduleService,
                          ProposalService proposalService, MaterialService materialService,
                          EstadoSemanalService estadoSemanalService) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.proposalService = proposalService;
        this.materialService = materialService;
        this.estadoSemanalService = estadoSemanalService;
    }

    /** Dashboard con estadisticas globales. Acepta ?modoEdicion=true para que el boton
     *  "Editar este panel" del editor visual reutilice esta misma ruta real (el admin ya
     *  puede verla sin trucos de otro usuario, a diferencia de los otros 3 portales). */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@RequestParam(required = false, defaultValue = "false") boolean modoEdicion, Model model) {
        model.addAttribute("modoEdicion", modoEdicion);
        model.addAttribute("totalTeachers", userService.countByRole(Role.TEACHER));
        model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
        model.addAttribute("totalPracticantes", userService.countByRole(Role.PRACTICANTE));
        model.addAttribute("totalAdmins", userService.countByRole(Role.ADMIN));
        model.addAttribute("totalUsers", userService.countAll());
        model.addAttribute("pendingProposals", proposalService.countByStatus(ProposalStatus.PENDING));
        model.addAttribute("approvedProposals", proposalService.countByStatus(ProposalStatus.APPROVED));
        model.addAttribute("rejectedProposals", proposalService.countByStatus(ProposalStatus.REJECTED));
        model.addAttribute("totalMaterials", materialService.countAll());
        model.addAttribute("totalSchedules", scheduleService.countActiveSchedules());
        model.addAttribute("recentProposals", proposalService.findRecentPending());
        model.addAttribute("recentUsers", userService.findRecentUsers());

        // Ocupacion global: plazas ocupadas frente a plazas ofertadas
        List<Schedule> allSchedules = scheduleService.findAll();
        long seatsTotal = 0;
        long seatsTaken = 0;
        for (Schedule s : allSchedules) {
            seatsTotal += s.getMaxStudents();
            seatsTaken += scheduleService.countActiveEnrollments(s);
        }
        model.addAttribute("seatsTotal", seatsTotal);
        model.addAttribute("seatsTaken", seatsTaken);
        model.addAttribute("upcomingSchedules",
                allSchedules.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("scheduleService", scheduleService);
        return "admin/dashboard";
    }

    /** Gestion de propuestas con filtros. */
    @GetMapping("/proposals")
    public String proposals(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String type,
                            Model model) {
        List<Proposal> proposals = proposalService.findAll();
        if (status != null && !status.isEmpty()) {
            try {
                ProposalStatus st = ProposalStatus.valueOf(status);
                proposals = proposals.stream().filter(p -> p.getStatus() == st).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        if (type != null && !type.isEmpty()) {
            try {
                ProposalType tp = ProposalType.valueOf(type);
                proposals = proposals.stream().filter(p -> p.getType() == tp).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        model.addAttribute("proposals", proposals);
        model.addAttribute("statusFilter", status);
        model.addAttribute("typeFilter", type);
        model.addAttribute("statuses", ProposalStatus.values());
        model.addAttribute("types", ProposalType.values());
        return "admin/proposals";
    }

    /** Aprobar o rechazar una propuesta. */
    @PostMapping("/proposals/{id}/review")
    public String reviewProposal(@PathVariable Long id,
                                 @RequestParam String decision,
                                 @RequestParam(required = false) String adminComment,
                                 RedirectAttributes ra) {
        if (adminComment == null || adminComment.trim().isEmpty()) {
            ra.addFlashAttribute("errorKey", "proposal.comment.required");
            return "redirect:/admin/proposals";
        }
        ProposalStatus newStatus = "APPROVE".equalsIgnoreCase(decision)
                ? ProposalStatus.APPROVED : ProposalStatus.REJECTED;
        proposalService.review(id, newStatus, adminComment);
        ra.addFlashAttribute("successKey", "proposal.reviewed");
        return "redirect:/admin/proposals";
    }

    /** Vista de todos los horarios con filtros (incluye buscar quien esta libre a una hora puntual). */
    @GetMapping("/schedules")
    public String schedules(@RequestParam(required = false) Long teacherId,
                            @RequestParam(required = false) String language,
                            @RequestParam(required = false) String dayFilter,
                            @RequestParam(required = false) String timeFilter,
                            Model model) {
        List<Schedule> schedules = scheduleService.findAll();
        if (teacherId != null) {
            schedules = schedules.stream()
                    .filter(s -> s.getTeacher().getId().equals(teacherId))
                    .collect(Collectors.toList());
        }
        if (language != null && !language.isEmpty()) {
            try {
                Language lang = Language.valueOf(language);
                schedules = schedules.stream().filter(s -> s.getLanguage() == lang).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        if (dayFilter != null && !dayFilter.isEmpty()) {
            try {
                DayOfWeekEnum dia = DayOfWeekEnum.valueOf(dayFilter);
                schedules = schedules.stream().filter(s -> s.getDayOfWeek() == dia).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        if (timeFilter != null && !timeFilter.isEmpty()) {
            try {
                java.time.LocalTime hora = java.time.LocalTime.parse(timeFilter);
                schedules = schedules.stream()
                        .filter(s -> !hora.isBefore(s.getStartTime()) && hora.isBefore(s.getEndTime()))
                        .collect(Collectors.toList());
            } catch (RuntimeException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        // Anexar conteo de inscritos
        model.addAttribute("schedules", schedules);
        model.addAttribute("scheduleService", scheduleService);
        model.addAttribute("teachers", userService.findByRole(Role.TEACHER));
        model.addAttribute("languages", Language.values());
        model.addAttribute("days", DayOfWeekEnum.values());
        model.addAttribute("teacherFilter", teacherId);
        model.addAttribute("languageFilter", language);
        model.addAttribute("dayFilter", dayFilter);
        model.addAttribute("timeFilter", timeFilter);
        model.addAttribute("canceladasEstaSemana", estadoSemanalService.canceladasEnSemana(estadoSemanalService.inicioDeSemanaActual()));
        return "admin/schedules";
    }

    /** Gestion de materiales con filtros. */
    @GetMapping("/materials")
    public String materials(@RequestParam(required = false) Long teacherId,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) String language,
                            Model model) {
        List<Material> materials = materialService.findAll();
        if (teacherId != null) {
            materials = materials.stream()
                    .filter(m -> m.getTeacher().getId().equals(teacherId))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isEmpty()) {
            try {
                MaterialCategory cat = MaterialCategory.valueOf(category);
                materials = materials.stream().filter(m -> m.getCategory() == cat).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        if (language != null && !language.isEmpty()) {
            try {
                Language lang = Language.valueOf(language);
                materials = materials.stream().filter(m -> m.getLanguage() == lang).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // valor de filtro invalido en la URL: se ignora en vez de tirar un 500
            }
        }
        model.addAttribute("materials", materials);
        model.addAttribute("teachers", userService.findByRole(Role.TEACHER));
        model.addAttribute("categories", MaterialCategory.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("teacherFilter", teacherId);
        model.addAttribute("categoryFilter", category);
        model.addAttribute("languageFilter", language);
        return "admin/materials";
    }

    /** Eliminar material inapropiado. */
    @PostMapping("/materials/{id}/delete")
    public String deleteMaterial(@PathVariable Long id, RedirectAttributes ra) {
        materialService.delete(id);
        ra.addFlashAttribute("successKey", "material.deleted");
        return "redirect:/admin/materials";
    }

    /** Gestion de usuarios. */
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    /** Cambiar el rol de un usuario. */
    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role,
                             java.security.Principal principal, RedirectAttributes ra) {
        Role nuevoRol;
        try {
            nuevoRol = Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorKey", "user.role.invalido");
            return "redirect:/admin/users";
        }
        User target = userService.findById(id).orElse(null);
        if (target != null && nuevoRol != Role.ADMIN
                && target.getEmail().equalsIgnoreCase(principal.getName())) {
            // Evita que un administrador se quite a si mismo el rol ADMIN por accidente
            // (mismo riesgo que deshabilitarse a si mismo: si es el unico admin, nadie
            // podria volver a entrar al panel de administracion para revertirlo).
            ra.addFlashAttribute("errorKey", "user.role.self.forbidden");
            return "redirect:/admin/users";
        }
        userService.updateRole(id, nuevoRol);
        ra.addFlashAttribute("successKey", "user.role.updated");
        return "redirect:/admin/users";
    }

    /** Habilitar o deshabilitar una cuenta: forma directa de cortar el acceso de alguien. */
    @PostMapping("/users/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled,
                                java.security.Principal principal, RedirectAttributes ra) {
        User target = userService.findById(id).orElse(null);
        if (target != null && !enabled && target.getEmail().equalsIgnoreCase(principal.getName())) {
            // Evita que un administrador se bloquee a si mismo por accidente.
            ra.addFlashAttribute("errorKey", "user.disable.self.forbidden");
            return "redirect:/admin/users";
        }
        userService.setEnabled(id, enabled);
        ra.addFlashAttribute("successKey", enabled ? "user.enabled.updated" : "user.disabled.updated");
        return "redirect:/admin/users";
    }
}
