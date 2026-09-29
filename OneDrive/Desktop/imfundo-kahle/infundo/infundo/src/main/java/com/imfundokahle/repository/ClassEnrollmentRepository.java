package com.imfundokahle.repository;

import com.imfundokahle.model.ClassEnrollment;
import com.imfundokahle.model.EnrollmentStatus;
import com.imfundokahle.model.Schedule;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** Repositorio de inscripciones a clases. */
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {
    List<ClassEnrollment> findByStudent(User student);
    List<ClassEnrollment> findByStudentAndStatus(User student, EnrollmentStatus status);
    List<ClassEnrollment> findBySchedule(Schedule schedule);
    List<ClassEnrollment> findByScheduleAndStatus(Schedule schedule, EnrollmentStatus status);
    Optional<ClassEnrollment> findByStudentAndSchedule(User student, Schedule schedule);
    long countByScheduleAndStatus(Schedule schedule, EnrollmentStatus status);
    long countByStudentAndStatus(User student, EnrollmentStatus status);
}
