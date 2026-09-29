package com.imfundokahle.repository;

import com.imfundokahle.model.Asistencia;
import com.imfundokahle.model.Schedule;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    Optional<Asistencia> findByScheduleAndStudentAndFecha(Schedule schedule, User student, LocalDate fecha);

    List<Asistencia> findByScheduleAndFecha(Schedule schedule, LocalDate fecha);

    List<Asistencia> findByScheduleAndStudentOrderByFechaDesc(Schedule schedule, User student);

    long countByScheduleAndStudentAndPresenteTrue(Schedule schedule, User student);

    long countByScheduleAndStudent(Schedule schedule, User student);
}
