package com.imfundokahle.service;

import com.imfundokahle.model.Horario;
import com.imfundokahle.model.SemanaFijada;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.SemanaFijadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * "Fijar mi semana": una vez que un profesor o practicante marco en la
 * cuadricula de Mi disponibilidad (ver HorarioService) sus horas libres
 * (verde) u ocupadas (rojo, modalidad "OCUPADO" con actividad obligatoria),
 * presiona "Fijar" para: 1) copiar la actividad de cada hora ocupada al
 * reporte semanal de horas (ver ReporteSemanalService), y 2) bloquear la
 * cuadricula esa semana (no se puede editar hasta "Editar de nuevo").
 */
@Service
public class DisponibilidadSemanalService {

    public static final String OCUPADO = "OCUPADO";

    private final SemanaFijadaRepository semanaFijadaRepository;
    private final ReporteSemanalService reporteSemanalService;

    public DisponibilidadSemanalService(SemanaFijadaRepository semanaFijadaRepository,
                                        ReporteSemanalService reporteSemanalService) {
        this.semanaFijadaRepository = semanaFijadaRepository;
        this.reporteSemanalService = reporteSemanalService;
    }

    public LocalDate inicioDeSemana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public LocalDate inicioDeSemanaActual() {
        return inicioDeSemana(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public boolean estaFijada(User colaborador, LocalDate semanaInicio) {
        return semanaFijadaRepository.findByColaboradorAndSemanaInicio(colaborador, semanaInicio).isPresent();
    }

    /**
     * Fija la semana: copia la actividad de cada hora en rojo (OCUPADO) al
     * reporte semanal, y bloquea la cuadricula. "horarioActual" es el horario
     * efectivo tal como esta en este momento (ver HorarioService.obtenerHorarioEfectivo).
     */
    @Transactional
    public void fijarSemana(User colaborador, LocalDate semanaInicio, List<Horario> horarioActual) {
        for (Horario h : horarioActual) {
            if (OCUPADO.equals(h.getModalidad()) && h.getActividad() != null && !h.getActividad().isBlank()) {
                reporteSemanalService.agregarNotaAutomatica(colaborador, semanaInicio, h.getDiaSemana(), h.getActividad());
            }
        }
        SemanaFijada f = semanaFijadaRepository.findByColaboradorAndSemanaInicio(colaborador, semanaInicio)
                .orElseGet(SemanaFijada::new);
        f.setColaborador(colaborador);
        f.setSemanaInicio(semanaInicio);
        semanaFijadaRepository.save(f);
    }

    /** Desbloquea la cuadricula de esa semana para poder volver a editarla. */
    @Transactional
    public void reactivarSemana(User colaborador, LocalDate semanaInicio) {
        semanaFijadaRepository.deleteByColaboradorAndSemanaInicio(colaborador, semanaInicio);
    }
}
