package com.imfundokahle.service;

import com.imfundokahle.model.ClassEnrollment;
import com.imfundokahle.model.DayOfWeekEnum;
import com.imfundokahle.model.Schedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Manda un correo recordatorio a los alumnos inscritos ~30 minutos antes de
 * que empiece su clase (grupal o personalizada). Revisa todas las clases cada
 * 5 minutos; "ultimoRecordatorio" en Schedule evita mandarlo dos veces la
 * misma semana para la misma clase.
 * <p>
 * Nota operativa: en un host gratuito que "duerme" el servicio tras un rato
 * sin visitas (ej. Render free tier), este programador solo corre mientras la
 * app este despierta. Si nadie visita el sitio justo antes de una clase, el
 * recordatorio de esa clase puntual puede no salir. No es un bug del codigo,
 * es una limitacion del plan gratuito de hospedaje.
 */
@Component
public class ClaseReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClaseReminderScheduler.class);

    /** Cuantos minutos antes de la clase se manda el aviso. */
    private static final int MINUTOS_ANTES = 30;
    /** Margen alrededor del objetivo: un poco mas que el intervalo de sondeo (5 min),
     *  para no perder la ventana si el arranque del servidor no cae justo en un multiplo. */
    private static final int VENTANA_MINUTOS = 6;

    private final ScheduleService scheduleService;
    private final MailService mailService;

    public ClaseReminderScheduler(ScheduleService scheduleService, MailService mailService) {
        this.scheduleService = scheduleService;
        this.mailService = mailService;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void enviarRecordatorios() {
        LocalDate hoy = LocalDate.now();
        DayOfWeek hoyIso = hoy.getDayOfWeek();
        LocalTime ahora = LocalTime.now();

        for (Schedule s : scheduleService.findAll()) {
            if (hoy.equals(s.getUltimoRecordatorio())) {
                continue; // ya se mando hoy para esta clase
            }
            if (!coincideDia(s.getDayOfWeek(), hoyIso) || s.getStartTime() == null) {
                continue;
            }
            LocalTime objetivo = s.getStartTime().minusMinutes(MINUTOS_ANTES);
            long diffMinutos = Math.abs(Duration.between(ahora, objetivo).toMinutes());
            if (diffMinutos > VENTANA_MINUTOS) {
                continue;
            }

            List<ClassEnrollment> enrollments = scheduleService.findEnrollmentsBySchedule(s);
            if (enrollments.isEmpty()) {
                continue;
            }
            for (ClassEnrollment e : enrollments) {
                mailService.enviarRecordatorioClase(
                        e.getStudent().getEmail(), s.getSubject(), s.getStartTime().toString(), s.getMeetLink());
            }
            s.setUltimoRecordatorio(hoy);
            scheduleService.save(s);
            log.info("Recordatorio de clase enviado: '{}' ({} alumnos)", s.getSubject(), enrollments.size());
        }
    }

    private boolean coincideDia(DayOfWeekEnum dia, DayOfWeek hoyIso) {
        if (dia == null) {
            return false;
        }
        return switch (dia) {
            case LUNES -> hoyIso == DayOfWeek.MONDAY;
            case MARTES -> hoyIso == DayOfWeek.TUESDAY;
            case MIERCOLES -> hoyIso == DayOfWeek.WEDNESDAY;
            case JUEVES -> hoyIso == DayOfWeek.THURSDAY;
            case VIERNES -> hoyIso == DayOfWeek.FRIDAY;
        };
    }
}
