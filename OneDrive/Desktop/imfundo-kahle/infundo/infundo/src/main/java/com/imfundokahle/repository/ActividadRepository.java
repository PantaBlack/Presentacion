package com.imfundokahle.repository;

import com.imfundokahle.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    
    // Spring Data JPA crea la consulta SQL automáticamente basándose en el nombre del método
    List<Actividad> findAllByOrderByCreadoEnDesc();
    
    List<Actividad> findByEstadoActualOrderByCreadoEnDesc(String estadoActual);

    List<Actividad> findByRolOrderByCreadoEnDesc(String rol);

    // Actividades asignadas a una persona (se cruzan por nombre completo, sin distinguir mayusculas).
    // Se conserva por compatibilidad con actividades viejas sin usuarioId; el cruce correcto
    // para "mis actividades" es por id (ver findByUsuarioIdOrderByCreadoEnDesc).
    List<Actividad> findByNombreAsignadoIgnoreCaseOrderByCreadoEnDesc(String nombreAsignado);

    // Cruce correcto por persona: por id de usuario, no por texto del nombre (dos personas
    // con el mismo nombre, o mayusculas distintas, ya no se mezclan entre si).
    List<Actividad> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
}