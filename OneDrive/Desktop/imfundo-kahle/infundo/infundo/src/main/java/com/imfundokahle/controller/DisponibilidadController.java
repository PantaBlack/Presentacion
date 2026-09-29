package com.imfundokahle.controller;

import com.imfundokahle.model.Horario;
import com.imfundokahle.model.Role;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.UserRepository;
import com.imfundokahle.service.HorarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/disponibilidad")
public class DisponibilidadController {

    /** Un profesor o practicante, ya resuelto a "disponible" o no para el dia+hora consultado. */
    public static class PersonaHora {
        private final User usuario;
        private final boolean disponible;
        private final String detalle;

        public PersonaHora(User usuario, boolean disponible, String detalle) {
            this.usuario = usuario;
            this.disponible = disponible;
            this.detalle = detalle;
        }

        public User getUsuario() { return usuario; }
        public boolean isDisponible() { return disponible; }
        public String getDetalle() { return detalle; }
    }

    private final HorarioService horarioService;
    private final UserRepository userRepository;

    public DisponibilidadController(HorarioService horarioService, UserRepository userRepository) {
        this.horarioService = horarioService;
        this.userRepository = userRepository;
    }

    // 1. MUESTRA LA LISTA Y EL CRUCE POR DIA + HORA (solo profesor/practicante: para
    // decidir a quien asignarle un alumno o una tarea a esa hora exacta).
    @GetMapping
    public String listaUsuarios(@RequestParam(required = false) String diaFiltro,
                                @RequestParam(required = false) String horaFiltro,
                                Model model) {
        model.addAttribute("titulo", "Cruce de Disponibilidad");
        model.addAttribute("activeTab", "disponibilidad");

        // El alumno no gestiona disponibilidad propia (solo recibe clases y horarios
        // ya asignados), asi que no tiene sentido cruzarlo aqui.
        List<User> todosLosUsuarios = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN && u.getRole() != Role.STUDENT)
                .collect(Collectors.toList());

        model.addAttribute("dias", HorarioService.DIAS_SEMANA);
        model.addAttribute("horas", HorarioService.HORAS_DEL_DIA);
        model.addAttribute("diaFiltro", diaFiltro);
        model.addAttribute("horaFiltro", horaFiltro);

        boolean hayDiaYHora = diaFiltro != null && !diaFiltro.isEmpty() && horaFiltro != null && !horaFiltro.isEmpty();
        if (hayDiaYHora) {
            List<PersonaHora> cruceHora = new ArrayList<>();
            for (User u : todosLosUsuarios) {
                // "Efectivo" incluye el almuerzo obligatorio de 13:00, aunque no este guardado.
                String modalidad = "LIBRE";
                String actividad = null;
                for (Horario h : horarioService.obtenerHorarioEfectivo(u)) {
                    if (h.getDiaSemana().equalsIgnoreCase(diaFiltro) && h.getHoraInicio().equals(horaFiltro)) {
                        modalidad = h.getModalidad();
                        actividad = h.getActividad();
                        break;
                    }
                }
                boolean disponible = "PRESENCIAL".equals(modalidad) || "VIRTUAL".equals(modalidad);
                String detalle;
                if (disponible) {
                    detalle = null;
                } else if ("ALMUERZO".equals(modalidad)) {
                    detalle = "ALMUERZO";
                } else if ("OCUPADO".equals(modalidad)) {
                    detalle = (actividad != null && !actividad.isBlank()) ? actividad : "OCUPADO";
                } else {
                    detalle = "SIN_MARCAR";
                }
                cruceHora.add(new PersonaHora(u, disponible, detalle));
            }
            // Profesores y practicantes libres primero, para verlos de un vistazo sin buscar.
            cruceHora.sort(Comparator.comparing(PersonaHora::isDisponible).reversed()
                    .thenComparing(p -> p.getUsuario().getFullName()));
            model.addAttribute("cruceHora", cruceHora);
        }

        model.addAttribute("usuarios", todosLosUsuarios);
        return "admin/disponibilidad-list";
    }

    // 2. MUESTRA EL EXCEL DE UN USUARIO ESPECÍFICO (Se mantiene intacto)
    @GetMapping("/inspeccionar/{id}")
    public String inspeccionarHorario(@PathVariable Long id, Model model) {
        User usuario = userRepository.findById(id).orElse(null);
        if (usuario == null) {
            return "redirect:/admin/disponibilidad";
        }

        model.addAttribute("titulo", "Horario de " + usuario.getFirstName());
        model.addAttribute("activeTab", "disponibilidad");
        model.addAttribute("usuario", usuario);

        List<String> horas = Arrays.asList("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00");
        List<String> dias = Arrays.asList("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES");

        // "Efectivo" incluye el almuerzo obligatorio de 13:00, aunque no este guardado.
        List<Horario> horarios = horarioService.obtenerHorarioEfectivo(usuario);
        Map<String, Map<String, String>> mapaHoras = new LinkedHashMap<>();
        
        for (String hora : horas) {
            Map<String, String> mapaDias = new LinkedHashMap<>();
            for (String dia : dias) {
                String modalidad = horarios.stream()
                        .filter(h -> h.getDiaSemana().equals(dia) && h.getHoraInicio().startsWith(hora))
                        .map(Horario::getModalidad)
                        .findFirst().orElse("");
                mapaDias.put(dia, modalidad);
            }
            mapaHoras.put(hora, mapaDias);
        }

        long totalPresencial = horarios.stream().filter(h -> "PRESENCIAL".equals(h.getModalidad())).count();
        long totalVirtual = horarios.stream().filter(h -> "VIRTUAL".equals(h.getModalidad())).count();

        model.addAttribute("mapaHoras", mapaHoras);
        model.addAttribute("horas", horas);
        model.addAttribute("dias", dias);
        model.addAttribute("totalPresencial", totalPresencial);
        model.addAttribute("totalVirtual", totalVirtual);
        model.addAttribute("sinDisponibilidad", (totalPresencial + totalVirtual) == 0);

        return "admin/disponibilidad-detalle";
    }
}