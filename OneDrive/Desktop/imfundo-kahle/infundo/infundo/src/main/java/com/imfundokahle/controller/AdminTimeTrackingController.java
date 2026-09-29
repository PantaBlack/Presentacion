package com.imfundokahle.controller;

import com.imfundokahle.model.EstadoReporte;
import com.imfundokahle.model.ReporteSemanal;
import com.imfundokahle.service.HorarioService;
import com.imfundokahle.service.ReporteSemanalService;
import com.imfundokahle.service.TimeTrackingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Panel del administrador para revisar los reportes semanales de horas:
 * comparativa [horas cronometradas por el sistema] vs [horas justificadas por
 * el colaborador], con poder absoluto para fijar las horas finales al aprobar,
 * o devolver el reporte a borrador con un comentario de correccion.
 */
@Controller
@RequestMapping("/admin/tiempo/reportes")
public class AdminTimeTrackingController {

    private final ReporteSemanalService reporteSemanalService;
    private final TimeTrackingService timeTrackingService;

    public AdminTimeTrackingController(ReporteSemanalService reporteSemanalService,
                                       TimeTrackingService timeTrackingService) {
        this.reporteSemanalService = reporteSemanalService;
        this.timeTrackingService = timeTrackingService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String estado, Model model) {
        model.addAttribute("titulo", "Reportes de tiempo");
        model.addAttribute("activeTab", "tiempo");

        List<ReporteSemanal> reportes;
        EstadoReporte filtro = null;
        if (estado != null && !estado.isBlank()) {
            try {
                filtro = EstadoReporte.valueOf(estado);
            } catch (IllegalArgumentException e) {
                filtro = null; // valor de filtro invalido: se ignora en vez de tirar un 500
            }
        }
        reportes = (filtro != null) ? reporteSemanalService.findByEstado(filtro) : reporteSemanalService.findAll();

        model.addAttribute("reportes", reportes);
        model.addAttribute("estadoFiltro", filtro);
        model.addAttribute("estados", EstadoReporte.values());
        model.addAttribute("countPendientes", reporteSemanalService.countByEstado(EstadoReporte.PENDIENTE_REVISION));
        // Se expone el propio servicio para que la tabla calcule las horas del sistema fila por fila
        // (mismo patron que scheduleService/materialService en los dashboards existentes).
        model.addAttribute("reporteSemanalService", reporteSemanalService);
        return "admin/tiempo-list";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        ReporteSemanal reporte = reporteSemanalService.findById(id);
        if (reporte == null) {
            return "redirect:/admin/tiempo/reportes";
        }
        model.addAttribute("titulo", "Reporte de " + reporte.getColaborador().getFirstName());
        model.addAttribute("activeTab", "tiempo");
        model.addAttribute("reporte", reporte);
        java.util.Map<String, Long> minutosPorDia =
                reporteSemanalService.minutosCronometradosPorDia(reporte.getColaborador(), reporte.getSemanaInicio());
        model.addAttribute("minutosPorDia", minutosPorDia);
        long minutosSistema = reporteSemanalService.minutosCronometradosSemana(
                reporte.getColaborador(), reporte.getSemanaInicio());
        model.addAttribute("minutosSistema", minutosSistema);
        long diasActivos = minutosPorDia.values().stream().filter(m -> m != null && m > 0).count();
        model.addAttribute("diasActivos", diasActivos);
        model.addAttribute("minutosPromedio", diasActivos > 0 ? minutosSistema / diasActivos : 0);
        model.addAttribute("minutosSugeridos",
                reporte.getMinutosFinalesAprobados() != null ? reporte.getMinutosFinalesAprobados() : minutosSistema);
        model.addAttribute("horas", HorarioService.HORAS_DEL_DIA);
        model.addAttribute("minutosPorHoraPorDia",
                timeTrackingService.minutosTrabajadosPorHoraPorDia(reporte.getColaborador(), reporte.getSemanaInicio()));
        return "admin/tiempo-detalle";
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Long id, @RequestParam int horasFinales,
                          @RequestParam(defaultValue = "0") int minutosFinalesExtra,
                          @RequestParam(required = false) String comentario, RedirectAttributes ra) {
        int minutosFinales = horasFinales * 60 + minutosFinalesExtra;
        boolean ok = reporteSemanalService.aprobar(id, minutosFinales, comentario);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey",
                ok ? "admin.tiempo.aprobado" : "admin.tiempo.accionInvalida");
        return "redirect:/admin/tiempo/reportes";
    }

    @PostMapping("/{id}/observar")
    public String observar(@PathVariable Long id, @RequestParam String comentario, RedirectAttributes ra) {
        boolean ok = reporteSemanalService.observar(id, comentario);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey",
                ok ? "admin.tiempo.observado" : "admin.tiempo.accionInvalida");
        return "redirect:/admin/tiempo/reportes";
    }
}
