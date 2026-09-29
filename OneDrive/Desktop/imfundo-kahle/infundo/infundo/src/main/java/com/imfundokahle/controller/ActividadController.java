package com.imfundokahle.controller;

import com.imfundokahle.model.Actividad;
import com.imfundokahle.model.Role;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.UserRepository;
import com.imfundokahle.service.ActividadService;
import com.imfundokahle.service.HorarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/actividades")
public class ActividadController {

    private final ActividadService actividadService;
    private final UserRepository userRepository;
    private final HorarioService horarioService;

    // Inyección de dependencias por constructor para limpiar la advertencia de VS Code
    public ActividadController(ActividadService actividadService, UserRepository userRepository,
                               HorarioService horarioService) {
        this.actividadService = actividadService;
        this.userRepository = userRepository;
        this.horarioService = horarioService;
    }

    @GetMapping("/panel")
    public String mostrarPanelActividades(Model model) {
        model.addAttribute("titulo", "Actividades y Horas");
        model.addAttribute("activeTab", "actividades");

        // Cualquier miembro de la comunidad (profesor, alumno o practicante) puede
        // recibir una actividad asignada; el administrador queda fuera de la lista.
        List<User> colaboradores = userRepository.findAll().stream()
            .filter(u -> u.getRole() == Role.TEACHER || u.getRole() == Role.STUDENT || u.getRole() == Role.PRACTICANTE)
            .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
            .collect(Collectors.toList());

        model.addAttribute("colaboradores", colaboradores);

        return "admin/panel-horas";
    }

    /**
     * Disponibilidad semanal de un colaborador (la misma que el llena en "Mi disponibilidad"),
     * para que el admin elija la tarea directamente sobre sus horas ya marcadas en vez de
     * escribirlas a mano.
     */
    @GetMapping("/api/disponibilidad/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Map<String, String>>> disponibilidadDeColaborador(@PathVariable Long userId) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(horarioService.obtenerMapaSemanal(u));
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<List<Actividad>> listarActividadesJson() {
        try {
            return new ResponseEntity<>(actividadService.obtenerTodasLasActividades(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Actividad> crearActividadJson(@RequestBody Actividad actividad) {
        try {
            return new ResponseEntity<>(actividadService.registrarNuevaActividad(actividad), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/api/actualizar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Actividad> actualizarEstadoActividad(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            return new ResponseEntity<>(actividadService.cambiarEstado(id, payload.get("estado")), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<HttpStatus> eliminarActividadJson(@PathVariable Long id) {
        try {
            actividadService.borrarActividad(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}