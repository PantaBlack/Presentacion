package com.imfundokahle.service;

import com.imfundokahle.model.Actividad;

import java.util.List;

/**
 * Resumen de las actividades asignadas a una persona: la lista completa mas
 * los conteos ya calculados, para que los controladores (y las plantillas)
 * no tengan que repetir la misma logica de agrupacion en cada portal.
 */
public record ActividadResumen(
        List<Actividad> actividades,
        long pendientes,
        long enCurso,
        long completadas,
        long horasPlanificadas,
        long horasCompletadas
) {
    public long total() {
        return actividades.size();
    }

    /** True si hay alguna tarea pendiente que la persona todavia no ha revisado/iniciado. */
    public boolean tienePendientes() {
        return pendientes > 0;
    }
}
