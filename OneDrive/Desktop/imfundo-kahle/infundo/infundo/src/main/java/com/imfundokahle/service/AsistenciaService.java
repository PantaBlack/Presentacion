package com.imfundokahle.service;

import com.imfundokahle.model.Asistencia;
import com.imfundokahle.model.Schedule;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.AsistenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asistencia de los alumnos a una clase, por fecha. Independiente de la
 * inscripcion (ClassEnrollment): la inscripcion dice "quien pertenece a la
 * clase", la asistencia dice "quien vino tal dia".
 */
@Service
public class AsistenciaService {

    private final AsistenciaRepository repository;

    public AsistenciaService(AsistenciaRepository repository) {
        this.repository = repository;
    }

    /** Marca (o corrige, si ya existia) la asistencia de un alumno en una fecha. */
    @Transactional
    public void marcar(Schedule schedule, User student, LocalDate fecha, boolean presente) {
        Asistencia a = repository.findByScheduleAndStudentAndFecha(schedule, student, fecha)
                .orElseGet(Asistencia::new);
        a.setSchedule(schedule);
        a.setStudent(student);
        a.setFecha(fecha);
        a.setPresente(presente);
        repository.save(a);
    }

    /** Mapa alumno-id -> presente, para pintar los checkboxes ya marcados de una fecha. */
    public Map<Long, Boolean> asistenciaDelDia(Schedule schedule, LocalDate fecha, List<User> alumnos) {
        Map<Long, Boolean> mapa = new LinkedHashMap<>();
        for (User alumno : alumnos) {
            repository.findByScheduleAndStudentAndFecha(schedule, alumno, fecha)
                    .ifPresent(a -> mapa.put(alumno.getId(), a.isPresente()));
        }
        return mapa;
    }

    public List<Asistencia> historialDe(Schedule schedule, User student) {
        return repository.findByScheduleAndStudentOrderByFechaDesc(schedule, student);
    }

    public long contarPresentes(Schedule schedule, User student) {
        return repository.countByScheduleAndStudentAndPresenteTrue(schedule, student);
    }

    public long contarTotal(Schedule schedule, User student) {
        return repository.countByScheduleAndStudent(schedule, student);
    }
}
