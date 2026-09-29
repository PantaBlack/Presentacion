package com.imfundokahle.service;

import com.imfundokahle.model.ClaseCanceladaSemana;
import com.imfundokahle.model.Schedule;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.ClaseCanceladaSemanaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado semanal de una clase de horario fijo: activa (verde, por defecto) o
 * cancelada esta semana puntual (rojo, con motivo), sin tocar el horario ni
 * las inscripciones. Cada semana vuelve a estar "activa" por defecto salvo
 * que se cancele de nuevo.
 */
@Service
public class EstadoSemanalService {

    private static final int MAX_MOTIVO = 500;

    private final ClaseCanceladaSemanaRepository repository;
    private final ReporteSemanalService reporteSemanalService;

    public EstadoSemanalService(ClaseCanceladaSemanaRepository repository, ReporteSemanalService reporteSemanalService) {
        this.repository = repository;
        this.reporteSemanalService = reporteSemanalService;
    }

    /** Lunes de la semana de "fecha" (si "fecha" ya es lunes, devuelve la misma fecha). */
    public LocalDate inicioDeSemana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public LocalDate inicioDeSemanaActual() {
        return inicioDeSemana(LocalDate.now());
    }

    /** Motivo de cancelacion de esta semana para esa clase, o null si esta activa. */
    public String motivoSiCancelada(Schedule schedule, LocalDate semanaInicio) {
        return repository.findByScheduleAndSemanaInicio(schedule, semanaInicio)
                .map(ClaseCanceladaSemana::getMotivo)
                .orElse(null);
    }

    /** Mapa scheduleId -> motivo, solo para las clases canceladas en esa semana (util para pintar la matriz del admin). */
    public Map<Long, String> canceladasEnSemana(LocalDate semanaInicio) {
        Map<Long, String> mapa = new LinkedHashMap<>();
        for (ClaseCanceladaSemana c : repository.findBySemanaInicio(semanaInicio)) {
            mapa.put(c.getSchedule().getId(), c.getMotivo());
        }
        return mapa;
    }

    /**
     * Marca una clase propia como cancelada esta semana (motivo obligatorio, se recorta
     * si es muy largo). Devuelve false si la clase no existe o no le pertenece al que la pide.
     */
    @Transactional
    public boolean cancelarSemana(Schedule schedule, User owner, LocalDate semanaInicio, String motivo) {
        if (schedule == null || !schedule.getTeacher().getId().equals(owner.getId())) {
            return false;
        }
        if (motivo == null || motivo.isBlank()) {
            return false;
        }
        String motivoRecortado = motivo.length() > MAX_MOTIVO ? motivo.substring(0, MAX_MOTIVO) : motivo;
        ClaseCanceladaSemana c = repository.findByScheduleAndSemanaInicio(schedule, semanaInicio)
                .orElseGet(ClaseCanceladaSemana::new);
        c.setSchedule(schedule);
        c.setSemanaInicio(semanaInicio);
        c.setMotivo(motivoRecortado);
        repository.save(c);
        reporteSemanalService.agregarNotaAutomatica(owner, semanaInicio, schedule.getDayOfWeek().name(), motivoRecortado);
        return true;
    }

    /** Vuelve a marcar la clase como activa esta semana (borra la cancelacion, si existia). */
    @Transactional
    public boolean reactivarSemana(Schedule schedule, User owner, LocalDate semanaInicio) {
        if (schedule == null || !schedule.getTeacher().getId().equals(owner.getId())) {
            return false;
        }
        repository.deleteByScheduleAndSemanaInicio(schedule, semanaInicio);
        return true;
    }
}
