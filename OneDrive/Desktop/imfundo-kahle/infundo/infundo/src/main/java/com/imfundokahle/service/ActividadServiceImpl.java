package com.imfundokahle.service;

import com.imfundokahle.model.Actividad;
import com.imfundokahle.repository.ActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ActividadServiceImpl implements ActividadService {

    private final ActividadRepository actividadRepository;

    // Inyección de dependencias por constructor (elimina la advertencia de VS Code)
    public ActividadServiceImpl(ActividadRepository actividadRepository) {
        this.actividadRepository = actividadRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Actividad> obtenerTodasLasActividades() {
        return actividadRepository.findAllByOrderByCreadoEnDesc();
    }

    @Override
    @Transactional
    public Actividad registrarNuevaActividad(Actividad actividad) {
        // Validaciones de negocio podrían ir aquí antes de guardar
        return actividadRepository.save(actividad);
    }

    @Override
    @Transactional(readOnly = true)
    public Actividad buscarPorId(Long id) {
        return actividadRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Actividad cambiarEstado(Long id, String nuevoEstado) {
        Optional<Actividad> actividadExistente = actividadRepository.findById(id);
        if (actividadExistente.isPresent()) {
            Actividad actividad = actividadExistente.get();
            actividad.setEstadoActual(nuevoEstado);
            return actividadRepository.save(actividad);
        }
        throw new RuntimeException("Actividad no encontrada con el ID: " + id);
    }

    @Override
    @Transactional
    public void borrarActividad(Long id) {
        actividadRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ActividadResumen obtenerResumenPara(Long usuarioId) {
        List<Actividad> actividades = usuarioId == null
                ? List.of()
                : actividadRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId);

        long pendientes = contarPorEstado(actividades, "Pendiente");
        long enCurso = contarPorEstado(actividades, "En curso");
        long completadas = contarPorEstado(actividades, "Completada");
        long horasPlanificadas = sumarHoras(actividades, null);
        long horasCompletadas = sumarHoras(actividades, "Completada");

        return new ActividadResumen(actividades, pendientes, enCurso, completadas, horasPlanificadas, horasCompletadas);
    }

    private long contarPorEstado(List<Actividad> actividades, String estado) {
        return actividades.stream()
                .filter(a -> estado.equalsIgnoreCase(a.getEstadoActual()))
                .count();
    }

    private long sumarHoras(List<Actividad> actividades, String estadoOFiltro) {
        return actividades.stream()
                .filter(a -> estadoOFiltro == null || estadoOFiltro.equalsIgnoreCase(a.getEstadoActual()))
                .filter(a -> a.getHorasPlanificadas() != null)
                .mapToLong(Actividad::getHorasPlanificadas)
                .sum();
    }
}