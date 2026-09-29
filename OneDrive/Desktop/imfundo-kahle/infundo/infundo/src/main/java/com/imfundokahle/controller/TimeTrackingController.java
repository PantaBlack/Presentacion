package com.imfundokahle.controller;

import com.imfundokahle.model.RegistroTiempo;
import com.imfundokahle.model.ReporteSemanal;
import com.imfundokahle.model.User;
import com.imfundokahle.service.HorarioService;
import com.imfundokahle.service.ReporteSemanalService;
import com.imfundokahle.service.TimeTrackingService;
import com.imfundokahle.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Control de tiempo propio: iniciar/pausar/reanudar/finalizar turno y
 * redactar el reporte semanal. Accesible a profesores, alumnos y practicantes
 * (mismos roles que "Mi disponibilidad"): cada quien fichea y reporta solo lo
 * suyo, sin ninguna validacion cruzada entre usuarios.
 */
@Controller
@RequestMapping("/mi-tiempo")
public class TimeTrackingController {

    private final UserService userService;
    private final TimeTrackingService timeTrackingService;
    private final ReporteSemanalService reporteSemanalService;

    public TimeTrackingController(UserService userService, TimeTrackingService timeTrackingService,
                                  ReporteSemanalService reporteSemanalService) {
        this.userService = userService;
        this.timeTrackingService = timeTrackingService;
        this.reporteSemanalService = reporteSemanalService;
    }

    private User current(Principal principal) {
        return userService.findByEmail(principal.getName()).orElseThrow();
    }

    @GetMapping
    public String ver(Principal principal, Model model) {
        User user = current(principal);
        LocalDate semanaInicio = ReporteSemanalService.lunesDeLaSemana(LocalDate.now());

        model.addAttribute("activeRole", user.getRole().name());
        model.addAttribute("turnoActivo", timeTrackingService.turnoActivo(user).orElse(null));
        model.addAttribute("historial", timeTrackingService.historial(user).stream().limit(10).toList());

        ReporteSemanal reporte = reporteSemanalService.obtenerOCrearBorrador(user, semanaInicio);
        model.addAttribute("reporte", reporte);
        model.addAttribute("semanaInicio", semanaInicio);
        model.addAttribute("minutosPorDia", reporteSemanalService.minutosCronometradosPorDia(user, semanaInicio));
        model.addAttribute("minutosSemana", reporteSemanalService.minutosCronometradosSemana(user, semanaInicio));
        model.addAttribute("plazoEnvio", ReporteSemanalService.plazoDeEnvio(semanaInicio));
        model.addAttribute("plazoVencido", reporteSemanalService.plazoVencido(semanaInicio));
        model.addAttribute("horas", HorarioService.HORAS_DEL_DIA);
        model.addAttribute("horasTrabajadasPorDia", timeTrackingService.horasTrabajadasPorDia(user, semanaInicio));
        model.addAttribute("chipKeys", List.of("clases", "reunion", "preparacion", "administrativo", "correccion", "capacitacion"));

        List<ReporteSemanal> anteriores = reporteSemanalService.findByColaborador(user).stream()
                .filter(r -> !r.getSemanaInicio().equals(semanaInicio))
                .limit(8)
                .toList();
        model.addAttribute("reportesAnteriores", anteriores);

        return "shared/mi-tiempo";
    }

    /** Poll de AJAX para el widget del reloj: siempre lee de la base de datos, nunca del navegador. */
    @GetMapping("/estado")
    @ResponseBody
    public Map<String, Object> estado(Principal principal) {
        User user = current(principal);
        Optional<RegistroTiempo> turno = timeTrackingService.turnoActivo(user);
        if (turno.isEmpty()) {
            return Map.of("activo", false);
        }
        RegistroTiempo t = turno.get();
        return Map.of(
                "activo", true,
                "estado", t.getEstado().name(),
                "minutosTrabajados", t.getMinutosTrabajados(),
                "cierreForzado", t.isCierreForzado()
        );
    }

    @PostMapping("/iniciar")
    public String iniciar(Principal principal) {
        timeTrackingService.iniciarTurno(current(principal));
        return "redirect:/mi-tiempo";
    }

    @PostMapping("/reanudar")
    public String reanudar(Principal principal) {
        timeTrackingService.reanudarTurno(current(principal));
        return "redirect:/mi-tiempo";
    }

    @PostMapping("/finalizar")
    public String finalizar(Principal principal) {
        timeTrackingService.finalizarTurno(current(principal));
        return "redirect:/mi-tiempo";
    }

    @PostMapping("/reportes/guardar")
    public String guardarReporte(Principal principal,
                                 @RequestParam String semanaInicio,
                                 @RequestParam(required = false) String notasLunes,
                                 @RequestParam(required = false) String notasMartes,
                                 @RequestParam(required = false) String notasMiercoles,
                                 @RequestParam(required = false) String notasJueves,
                                 @RequestParam(required = false) String notasViernes,
                                 @RequestParam(required = false) Integer minutosAjusteSolicitado,
                                 @RequestParam(required = false) String comentarioAjuste,
                                 RedirectAttributes ra) {
        LocalDate lunes;
        try {
            lunes = LocalDate.parse(semanaInicio);
        } catch (java.time.format.DateTimeParseException e) {
            ra.addFlashAttribute("errorKey", "mitiempo.reporte.fechaInvalida");
            return "redirect:/mi-tiempo";
        }
        // El hidden del formulario siempre manda el lunes calculado por el servidor: si no
        // cuadra (formulario manipulado a mano), no se guarda nada en vez de crear un
        // reporte "de la semana" que en realidad empieza cualquier otro dia.
        if (!lunes.equals(ReporteSemanalService.lunesDeLaSemana(lunes))) {
            ra.addFlashAttribute("errorKey", "mitiempo.reporte.fechaInvalida");
            return "redirect:/mi-tiempo";
        }
        reporteSemanalService.guardarBorrador(current(principal), lunes,
                notasLunes, notasMartes, notasMiercoles, notasJueves, notasViernes,
                minutosAjusteSolicitado, comentarioAjuste);
        ra.addFlashAttribute("successKey", "mitiempo.reporte.guardado");
        return "redirect:/mi-tiempo";
    }

    @PostMapping("/reportes/{id}/enviar")
    public String enviarReporte(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        String resultado = reporteSemanalService.enviar(id, current(principal));
        switch (resultado) {
            case "ok" -> ra.addFlashAttribute("successKey", "mitiempo.reporte.enviado");
            case "vencido" -> ra.addFlashAttribute("errorKey", "mitiempo.reporte.enviarError.plazoVencido");
            default -> ra.addFlashAttribute("errorKey", "mitiempo.reporte.enviarError");
        }
        return "redirect:/mi-tiempo";
    }
}
