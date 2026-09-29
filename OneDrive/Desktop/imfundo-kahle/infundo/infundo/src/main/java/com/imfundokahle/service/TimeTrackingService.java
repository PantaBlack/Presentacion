package com.imfundokahle.service;

import com.imfundokahle.model.EstadoTurno;
import com.imfundokahle.model.Horario;
import com.imfundokahle.model.RegistroTiempo;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.RegistroTiempoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Control de tiempo por timestamps del servidor: iniciar/pausar/reanudar/
 * finalizar turno, mas los dos jobs automaticos (corte de almuerzo a las
 * 13:00 y auto-checkout de quien olvido marcar salida).
 * <p>
 * Todo esta scoped por {@link User}: cada colaborador tiene su propio turno
 * activo (o ninguno), sin ninguna validacion cruzada entre usuarios. Como el
 * estado se lee siempre de la base de datos (nunca de la sesion del navegador),
 * abrir la app en dos dispositivos a la vez muestra el mismo turno corriendo
 * en vez de crear uno duplicado.
 */
@Service
public class TimeTrackingService {

    private static final Logger log = LoggerFactory.getLogger(TimeTrackingService.class);

    /** Estados en los que un turno todavia cuenta como "activo" (no finalizado). */
    private static final List<EstadoTurno> ACTIVOS = List.of(EstadoTurno.EN_TURNO, EstadoTurno.EN_REFRIGERIO);

    private final RegistroTiempoRepository registroTiempoRepository;
    private final HorarioService horarioService;

    public TimeTrackingService(RegistroTiempoRepository registroTiempoRepository, HorarioService horarioService) {
        this.registroTiempoRepository = registroTiempoRepository;
        this.horarioService = horarioService;
    }

    @Transactional(readOnly = true)
    public Optional<RegistroTiempo> turnoActivo(User colaborador) {
        return registroTiempoRepository.findFirstByColaboradorAndEstadoInOrderByHoraInicioDesc(colaborador, ACTIVOS);
    }

    /** Inicia un turno nuevo, o devuelve el que ya estuviera corriendo (idempotente). */
    @Transactional
    public RegistroTiempo iniciarTurno(User colaborador) {
        Optional<RegistroTiempo> activo = turnoActivo(colaborador);
        if (activo.isPresent()) {
            return activo.get();
        }
        RegistroTiempo nuevo = new RegistroTiempo(colaborador, LocalDateTime.now());
        return registroTiempoRepository.save(nuevo);
    }

    /** Reanuda el turno solo si esta en refrigerio; si no hay nada que reanudar, no hace nada. */
    @Transactional
    public Optional<RegistroTiempo> reanudarTurno(User colaborador) {
        Optional<RegistroTiempo> turno = turnoActivo(colaborador)
                .filter(t -> t.getEstado() == EstadoTurno.EN_REFRIGERIO);
        turno.ifPresent(t -> {
            t.setFinAlmuerzo(LocalDateTime.now());
            t.setEstado(EstadoTurno.EN_TURNO);
            registroTiempoRepository.save(t);
        });
        return turno;
    }

    /** Finaliza el turno activo (en curso o en refrigerio) del colaborador, si tiene uno. */
    @Transactional
    public Optional<RegistroTiempo> finalizarTurno(User colaborador) {
        Optional<RegistroTiempo> turno = turnoActivo(colaborador);
        turno.ifPresent(t -> {
            t.setHoraFin(LocalDateTime.now());
            t.setEstado(EstadoTurno.FINALIZADO);
            registroTiempoRepository.save(t);
        });
        return turno;
    }

    @Transactional(readOnly = true)
    public List<RegistroTiempo> historial(User colaborador) {
        return registroTiempoRepository.findByColaboradorOrderByFechaDescHoraInicioDesc(colaborador);
    }

    @Transactional(readOnly = true)
    public long minutosCronometradosSemana(User colaborador, LocalDate semanaInicio) {
        return registroTiempoRepository
                .findByColaboradorAndFechaBetween(colaborador, semanaInicio, semanaInicio.plusDays(4))
                .stream().mapToLong(RegistroTiempo::getMinutosTrabajados).sum();
    }

    /**
     * Que horas (de HorarioService.HORAS_DEL_DIA) quedaron realmente cubiertas
     * por un turno, en el mismo formato dia -> horas que usa la cuadricula de
     * disponibilidad: alimenta la matriz de solo lectura del reporte semanal,
     * para que el colaborador vea sus horas ya fichadas en vez de tener que
     * volver a escribirlas.
     */
    @Transactional(readOnly = true)
    public Map<String, Set<String>> horasTrabajadasPorDia(User colaborador, LocalDate semanaInicio) {
        Map<String, Set<String>> mapa = new LinkedHashMap<>();
        for (String dia : HorarioService.DIAS_SEMANA) {
            mapa.put(dia, new LinkedHashSet<>());
        }
        for (RegistroTiempo t : registroTiempoRepository
                .findByColaboradorAndFechaBetween(colaborador, semanaInicio, semanaInicio.plusDays(4))) {
            String dia = HorarioService.nombreDia(t.getFecha());
            if (dia == null) {
                continue;
            }
            LocalDateTime fin = t.getHoraFin() != null ? t.getHoraFin() : LocalDateTime.now();
            for (String horaStr : HorarioService.HORAS_DEL_DIA) {
                // La franja del almuerzo (13:00) nunca cuenta como trabajada si ese turno
                // en realidad hizo su pausa obligatoria ese dia.
                if (HorarioService.HORA_ALMUERZO_OBLIGATORIA.equals(horaStr) && t.getInicioAlmuerzo() != null) {
                    continue;
                }
                LocalDateTime bloqueInicio = t.getFecha().atTime(LocalTime.parse(horaStr));
                LocalDateTime bloqueFin = bloqueInicio.plusHours(1);
                if (bloqueInicio.isBefore(fin) && bloqueFin.isAfter(t.getHoraInicio())) {
                    mapa.get(dia).add(horaStr);
                }
            }
        }
        return mapa;
    }

    /**
     * Igual que {@link #horasTrabajadasPorDia}, pero en vez de solo marcar si
     * una hora quedo cubierta o no, calcula cuantos minutos reales (0-60) caen
     * dentro de cada bloque: alimenta la comparativa del admin para que el
     * ancho de cada barra sea proporcional a lo realmente fichado, en vez de
     * pintar la hora completa aunque el turno solo la haya cubierto a medias.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Integer>> minutosTrabajadosPorHoraPorDia(User colaborador, LocalDate semanaInicio) {
        Map<String, Map<String, Integer>> mapa = new LinkedHashMap<>();
        for (String dia : HorarioService.DIAS_SEMANA) {
            Map<String, Integer> horas = new LinkedHashMap<>();
            for (String horaStr : HorarioService.HORAS_DEL_DIA) {
                horas.put(horaStr, 0);
            }
            mapa.put(dia, horas);
        }
        for (RegistroTiempo t : registroTiempoRepository
                .findByColaboradorAndFechaBetween(colaborador, semanaInicio, semanaInicio.plusDays(4))) {
            String dia = HorarioService.nombreDia(t.getFecha());
            if (dia == null) {
                continue;
            }
            LocalDateTime fin = t.getHoraFin() != null ? t.getHoraFin() : LocalDateTime.now();
            Map<String, Integer> horas = mapa.get(dia);
            for (String horaStr : HorarioService.HORAS_DEL_DIA) {
                if (HorarioService.HORA_ALMUERZO_OBLIGATORIA.equals(horaStr) && t.getInicioAlmuerzo() != null) {
                    continue;
                }
                LocalDateTime bloqueInicio = t.getFecha().atTime(LocalTime.parse(horaStr));
                LocalDateTime bloqueFin = bloqueInicio.plusHours(1);
                LocalDateTime solapInicio = bloqueInicio.isAfter(t.getHoraInicio()) ? bloqueInicio : t.getHoraInicio();
                LocalDateTime solapFin = bloqueFin.isBefore(fin) ? bloqueFin : fin;
                if (solapInicio.isBefore(solapFin)) {
                    int minutosSolapados = (int) java.time.Duration.between(solapInicio, solapFin).toMinutes();
                    horas.merge(horaStr, Math.min(60, minutosSolapados), (a, b) -> Math.min(60, a + b));
                }
            }
        }
        return mapa;
    }

    /**
     * Hora en la que este turno deberia terminar segun la disponibilidad que el
     * colaborador marco para ese dia (la ultima franja PRESENCIAL/VIRTUAL/OCUPADO + 1h),
     * o medianoche si no marco nada ese dia (o cayo en fin de semana). El
     * auto-checkout usa esto en vez de un corte fijo a las 23:59, para que las
     * horas de un turno nocturno se sumen a la jornada correcta.
     */
    @Transactional(readOnly = true)
    public LocalDateTime horaFinProgramada(RegistroTiempo turno) {
        LocalDate fecha = turno.getFecha();
        LocalDateTime medianoche = fecha.plusDays(1).atStartOfDay();

        String diaSemana = HorarioService.nombreDia(fecha);
        if (diaSemana == null) {
            return medianoche;
        }

        String ultimaHora = null;
        for (Horario h : horarioService.obtenerHorarioEfectivo(turno.getColaborador())) {
            if (!diaSemana.equals(h.getDiaSemana())) {
                continue;
            }
            String modalidad = h.getModalidad();
            if (!"PRESENCIAL".equals(modalidad) && !"VIRTUAL".equals(modalidad) && !"OCUPADO".equals(modalidad)) {
                continue;
            }
            if (ultimaHora == null || h.getHoraInicio().compareTo(ultimaHora) > 0) {
                ultimaHora = h.getHoraInicio();
            }
        }
        if (ultimaHora == null) {
            return medianoche;
        }
        return LocalDateTime.of(fecha, LocalTime.parse(ultimaHora).plusHours(1));
    }

    /**
     * Regla de las 13:00: a esa hora en punto, todo turno EN_TURNO que haya
     * empezado hoy pasa a EN_REFRIGERIO. Independiente de a que hora entro cada
     * quien: es la misma hora de almuerzo obligatoria que ya rige en "Mi disponibilidad".
     */
    @Scheduled(cron = "0 0 13 * * *")
    @Transactional
    public void pausarPorAlmuerzo() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        List<RegistroTiempo> enTurno = registroTiempoRepository.findByEstadoIn(List.of(EstadoTurno.EN_TURNO));
        for (RegistroTiempo t : enTurno) {
            if (t.getFecha().equals(hoy)) {
                t.setInicioAlmuerzo(ahora);
                t.setEstado(EstadoTurno.EN_REFRIGERIO);
                registroTiempoRepository.save(t);
            }
        }
        if (!enTurno.isEmpty()) {
            log.info("Corte de almuerzo de las 13:00 aplicado a {} turno(s) activos", enTurno.size());
        }
    }

    /**
     * Auto-checkout: cierra por la fuerza cualquier turno que siga activo despues
     * de su hora de fin programada (o medianoche), y lo marca como cierre forzado
     * para que el administrador lo pueda auditar.
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    @Transactional
    public void autoCheckoutOlvidados() {
        LocalDateTime ahora = LocalDateTime.now();
        for (RegistroTiempo t : registroTiempoRepository.findByEstadoIn(ACTIVOS)) {
            LocalDateTime limite = horaFinProgramada(t);
            if (!ahora.isBefore(limite)) {
                t.setHoraFin(limite);
                t.setEstado(EstadoTurno.FINALIZADO);
                t.setCierreForzado(true);
                registroTiempoRepository.save(t);
                log.info("Auto-checkout forzado para el colaborador {} (turno del {})",
                        t.getColaborador().getEmail(), t.getFecha());
            }
        }
    }
}
