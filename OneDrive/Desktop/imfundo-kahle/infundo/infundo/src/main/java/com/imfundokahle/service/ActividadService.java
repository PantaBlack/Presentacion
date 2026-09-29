package com.imfundokahle.service;

import com.imfundokahle.model.Actividad;
import java.util.List;

public interface ActividadService {
    List<Actividad> obtenerTodasLasActividades();
    Actividad registrarNuevaActividad(Actividad actividad);
    Actividad buscarPorId(Long id);
    Actividad cambiarEstado(Long id, String nuevoEstado);
    void borrarActividad(Long id);

    /** Resumen de las actividades asignadas a una persona, por su id de usuario (no por nombre:
     *  dos personas con el mismo nombre no deben mezclarse entre si). */
    ActividadResumen obtenerResumenPara(Long usuarioId);
}