package com.imfundokahle.controller;

import com.imfundokahle.model.Actividad;
import com.imfundokahle.model.Horario;
import com.imfundokahle.model.User;
import com.imfundokahle.service.ActividadService;
import com.imfundokahle.service.DisponibilidadSemanalService;
import com.imfundokahle.service.HorarioService;
import com.imfundokahle.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Disponibilidad semanal propia: cada profesor o practicante marca aqui en
 * que horas puede atender y de que forma (presencial, virtual, su hora de
 * almuerzo, u ocupado con otra actividad), y "fija" su semana. Es la misma
 * tabla que despues ve el administrador, de forma consolidada, en
 * /admin/disponibilidad. El alumno no tiene acceso a esta ruta (ver
 * SecurityConfig): solo recibe las clases y horarios que se le asignan.
 */
@Controller
@RequestMapping("/mi-disponibilidad")
public class MiDisponibilidadController {

    private final UserService userService;
    private final HorarioService horarioService;
    private final ActividadService actividadService;
    private final DisponibilidadSemanalService disponibilidadSemanalService;

    public MiDisponibilidadController(UserService userService, HorarioService horarioService,
                                      ActividadService actividadService,
                                      DisponibilidadSemanalService disponibilidadSemanalService) {
        this.userService = userService;
        this.horarioService = horarioService;
        this.actividadService = actividadService;
        this.disponibilidadSemanalService = disponibilidadSemanalService;
    }

    private User current(Principal principal) {
        return userService.findByEmail(principal.getName()).orElseThrow();
    }

    @GetMapping
    public String ver(Principal principal, Model model) {
        User user = current(principal);
        model.addAttribute("dias", HorarioService.DIAS_SEMANA);
        model.addAttribute("horas", HorarioService.HORAS_DEL_DIA);
        model.addAttribute("celdas", horarioService.obtenerMapaSemanal(user));
        model.addAttribute("activeRole", user.getRole().name());

        // Actividad de las celdas marcadas "OCUPADO", para mostrarla en la cuadricula.
        model.addAttribute("actividadesOcupado", mapaActividadOcupado(horarioService.obtenerHorarioEfectivo(user)));

        // Que celdas tienen una actividad asignada por el admin, para resaltarlas en la cuadricula.
        List<Actividad> misActividades = actividadService.obtenerResumenPara(user.getId()).actividades();
        model.addAttribute("actividadesPorCelda", mapaActividadesPorCelda(misActividades));

        // "Fijar mi semana": si ya esta fijada, la cuadricula se ve bloqueada.
        model.addAttribute("semanaFijada",
                disponibilidadSemanalService.estaFijada(user, disponibilidadSemanalService.inicioDeSemanaActual()));

        return "shared/mi-disponibilidad";
    }

    /**
     * Fija la semana: copia la actividad de cada hora "OCUPADO" al reporte semanal de
     * horas (ver DisponibilidadSemanalService) y bloquea la cuadricula para que no se
     * pueda seguir editando hasta que se presione "Editar de nuevo".
     */
    @PostMapping("/fijar")
    public String fijarSemana(Principal principal, RedirectAttributes ra) {
        User user = current(principal);
        LocalDate semanaActual = disponibilidadSemanalService.inicioDeSemanaActual();
        disponibilidadSemanalService.fijarSemana(user, semanaActual, horarioService.obtenerHorarioEfectivo(user));
        ra.addFlashAttribute("successKey", "disponibilidad.semana.fijada");
        return "redirect:/mi-disponibilidad";
    }

    /** Desbloquea la cuadricula de la semana actual para poder volver a editarla. */
    @PostMapping("/reactivar")
    public String reactivarSemana(Principal principal, RedirectAttributes ra) {
        User user = current(principal);
        disponibilidadSemanalService.reactivarSemana(user, disponibilidadSemanalService.inicioDeSemanaActual());
        ra.addFlashAttribute("successKey", "disponibilidad.semana.reactivada");
        return "redirect:/mi-disponibilidad";
    }

    /**
     * Alterna el estado de una celda (AJAX): devuelve la modalidad que quedo guardada.
     * Si la semana ya esta fijada, o si la modalidad es OCUPADO sin actividad, no
     * guarda nada y devuelve el motivo para que el frontend pueda avisar.
     */
    @PostMapping("/celda")
    @ResponseBody
    public Map<String, String> actualizarCelda(Principal principal,
                                               @RequestParam String dia,
                                               @RequestParam String horaInicio,
                                               @RequestParam String modalidad,
                                               @RequestParam(required = false) String actividad) {
        User user = current(principal);

        String diaValido = HorarioService.DIAS_SEMANA.contains(dia) ? dia : null;
        String horaValida = HorarioService.HORAS_DEL_DIA.contains(horaInicio) ? horaInicio : null;
        String modalidadValida = HorarioService.CICLO_MODALIDADES.contains(modalidad) ? modalidad : null;

        if (diaValido == null || horaValida == null || modalidadValida == null) {
            return Map.of("ok", "false");
        }
        if (HorarioService.HORA_ALMUERZO_OBLIGATORIA.equals(horaValida)) {
            // El almuerzo es obligatorio para todos: no se puede editar ni con una peticion armada a mano.
            return Map.of("ok", "false", "reason", "almuerzo_obligatorio", "modalidad", HorarioService.MODALIDAD_ALMUERZO);
        }
        if (disponibilidadSemanalService.estaFijada(user, disponibilidadSemanalService.inicioDeSemanaActual())) {
            return Map.of("ok", "false", "reason", "semana_fijada");
        }

        boolean guardado = horarioService.marcarCelda(user, diaValido, horaValida, modalidadValida, actividad);
        if (!guardado) {
            return Map.of("ok", "false", "reason", "actividad_requerida");
        }
        return Map.of("ok", "true", "modalidad", modalidadValida);
    }

    /** Mapa dia -> hora -> actividad, solo para las celdas marcadas "OCUPADO". */
    private Map<String, Map<String, String>> mapaActividadOcupado(List<Horario> horarios) {
        Map<String, Map<String, String>> mapa = new LinkedHashMap<>();
        for (String dia : HorarioService.DIAS_SEMANA) {
            mapa.put(dia, new LinkedHashMap<>());
        }
        for (Horario h : horarios) {
            if (HorarioService.MODALIDAD_OCUPADO.equals(h.getModalidad()) && h.getActividad() != null) {
                mapa.get(h.getDiaSemana()).put(h.getHoraInicio(), h.getActividad());
            }
        }
        return mapa;
    }

    /**
     * Convierte la lista de actividades asignadas (dia + rango de horas libre) en un mapa
     * dia -> hora -> descripcion de la tarea, para poder resaltar la celda exacta en la
     * cuadricula. Las actividades antiguas sin dia o con un rango que no se pueda leer
     * como "HH:mm - HH:mm" simplemente no se resaltan (no hay celda concreta que marcar).
     */
    private Map<String, Map<String, String>> mapaActividadesPorCelda(List<Actividad> actividades) {
        Map<String, Map<String, String>> mapa = new LinkedHashMap<>();
        for (String dia : HorarioService.DIAS_SEMANA) {
            mapa.put(dia, new LinkedHashMap<>());
        }
        for (Actividad a : actividades) {
            if (a.getDiaSemana() == null || !mapa.containsKey(a.getDiaSemana()) || a.getRangoHorario() == null) {
                continue;
            }
            String[] partes = a.getRangoHorario().split("-");
            if (partes.length != 2) {
                continue;
            }
            try {
                LocalTime inicio = LocalTime.parse(partes[0].trim());
                LocalTime fin = LocalTime.parse(partes[1].trim());
                LocalTime cursor = inicio;
                while (cursor.isBefore(fin)) {
                    String hora = cursor.toString();
                    if (HorarioService.HORAS_DEL_DIA.contains(hora)) {
                        mapa.get(a.getDiaSemana()).put(hora, a.getTareaDetalle());
                    }
                    cursor = cursor.plusHours(1);
                }
            } catch (DateTimeParseException ignored) {
                // Rango escrito a mano en un formato distinto: no se puede cruzar celda a celda.
            }
        }
        return mapa;
    }
}
