package com.imfundokahle.service;

import com.imfundokahle.model.*;
import com.imfundokahle.repository.ClassEnrollmentRepository;
import com.imfundokahle.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de horarios e inscripciones.
 */
@Service
public class ScheduleService {

    // Deben coincidir con el "length" de cada columna (ver Schedule): recortar aqui,
    // en el unico lugar donde de verdad se persiste, evita que un texto pegado muy
    // largo tumbe el guardado con un error de base de datos, sin importar desde que
    // controlador (profesor o practicante) haya llegado.
    private static final int MAX_SUBJECT = 255;
    private static final int MAX_DESCRIPTION = 1000;
    private static final int MAX_MEET_LINK = 500;
    private static final int MAX_NOTA = 1000;

    private final ScheduleRepository scheduleRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           ClassEnrollmentRepository enrollmentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public Schedule save(Schedule schedule) {
        schedule.setSubject(recortar(schedule.getSubject(), MAX_SUBJECT));
        schedule.setDescription(recortar(schedule.getDescription(), MAX_DESCRIPTION));
        schedule.setMeetLink(recortar(schedule.getMeetLink(), MAX_MEET_LINK));
        return scheduleRepository.save(schedule);
    }

    private static String recortar(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }

    public Schedule findById(Long id) {
        return scheduleRepository.findById(id).orElse(null);
    }

    public List<Schedule> findAll() {
        return scheduleRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
    }

    /** Solo las clases de inscripcion abierta: excluye las personalizadas (1 a 1),
     *  que un alumno nunca deberia poder autoinscribirse ni ver en el listado publico. */
    public List<Schedule> findOpenForEnrollment() {
        return findAll().stream().filter(s -> !s.isPersonalizada()).collect(java.util.stream.Collectors.toList());
    }

    public List<Schedule> findByTeacher(User teacher) {
        return scheduleRepository.findByTeacherOrderByDayOfWeekAscStartTimeAsc(teacher);
    }

    public long countByTeacher(User teacher) {
        return scheduleRepository.countByTeacher(teacher);
    }

    /** Actualiza el enlace de videollamada de un horario propio. Devuelve false si no existe
     *  o no le pertenece al profesor (el llamador ya valida el formato del enlace). */
    @Transactional
    public boolean updateMeetLink(Long scheduleId, User teacher, String meetLink) {
        Schedule s = findById(scheduleId);
        if (s == null || !s.getTeacher().getId().equals(teacher.getId())) {
            return false;
        }
        s.setMeetLink(meetLink);
        save(s);
        return true;
    }

    /**
     * Edita dia/hora/materia/idioma/descripcion de un horario propio, sin necesidad de
     * borrarlo y recrearlo (lo que habria borrado tambien las inscripciones). Devuelve
     * false si no existe, no le pertenece al profesor, o si el nuevo cupo maximo
     * quedaria por debajo de los alumnos ya inscritos activamente.
     */
    @Transactional
    public boolean actualizar(Long scheduleId, User teacher, DayOfWeekEnum dayOfWeek,
                              java.time.LocalTime startTime, java.time.LocalTime endTime,
                              String subject, Language language, String description, int maxStudents) {
        Schedule s = findById(scheduleId);
        if (s == null || !s.getTeacher().getId().equals(teacher.getId())) {
            return false;
        }
        if (maxStudents < countActiveEnrollments(s)) {
            return false;
        }
        s.setDayOfWeek(dayOfWeek);
        s.setStartTime(startTime);
        s.setEndTime(endTime);
        s.setSubject(subject);
        s.setLanguage(language);
        s.setDescription(description);
        s.setMaxStudents(maxStudents);
        save(s);
        return true;
    }

    @Transactional
    public void delete(Long id) {
        Schedule s = findById(id);
        if (s != null) {
            // Eliminar inscripciones asociadas antes de borrar el horario
            List<ClassEnrollment> enrollments = enrollmentRepository.findBySchedule(s);
            enrollmentRepository.deleteAll(enrollments);
            scheduleRepository.delete(s);
        }
    }

    // ---- Inscripciones ----

    /** Numero de alumnos activos en un horario. */
    public long countActiveEnrollments(Schedule schedule) {
        return enrollmentRepository.countByScheduleAndStatus(schedule, EnrollmentStatus.ACTIVE);
    }

    /** Plazas disponibles en un horario. */
    public long availableSeats(Schedule schedule) {
        return schedule.getMaxStudents() - countActiveEnrollments(schedule);
    }

    public boolean isStudentEnrolled(User student, Schedule schedule) {
        return enrollmentRepository.findByStudentAndSchedule(student, schedule)
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .isPresent();
    }

    /** Inscribe a un alumno en un horario abierto si hay cupo y no esta ya inscrito.
     *  Las clases personalizadas (1 a 1) no se autoinscriben: las asigna directamente
     *  quien las crea (ver asignarAlumno). */
    @Transactional
    public String enroll(User student, Schedule schedule) {
        if (schedule.isPersonalizada()) {
            return "personalizada";
        }
        return doEnroll(student, schedule);
    }

    /** Asigna directamente a un alumno a una clase (usado por el profesor/practicante
     *  que crea una clase personalizada 1 a 1, que no pasa por autoinscripcion abierta). */
    @Transactional
    public String asignarAlumno(User student, Schedule schedule) {
        return doEnroll(student, schedule);
    }

    private String doEnroll(User student, Schedule schedule) {
        if (isStudentEnrolled(student, schedule)) {
            return "already";
        }
        if (availableSeats(schedule) <= 0) {
            return "full";
        }
        // Reutilizar inscripcion cancelada si existe
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentAndSchedule(student, schedule)
                .orElseGet(ClassEnrollment::new);
        enrollment.setStudent(student);
        enrollment.setSchedule(schedule);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        if (enrollment.getEnrolledAt() == null) {
            enrollment.setEnrolledAt(java.time.LocalDateTime.now());
        }
        enrollmentRepository.save(enrollment);
        return "ok";
    }

    @Transactional
    public void cancelEnrollment(User student, Schedule schedule) {
        enrollmentRepository.findByStudentAndSchedule(student, schedule).ifPresent(e -> {
            e.setStatus(EnrollmentStatus.CANCELLED);
            enrollmentRepository.save(e);
        });
    }

    public List<ClassEnrollment> findEnrollmentsByStudent(User student) {
        return enrollmentRepository.findByStudentAndStatus(student, EnrollmentStatus.ACTIVE);
    }

    public List<ClassEnrollment> findEnrollmentsBySchedule(Schedule schedule) {
        return enrollmentRepository.findByScheduleAndStatus(schedule, EnrollmentStatus.ACTIVE);
    }

    /** Guarda la nota de progreso de una inscripcion, solo si el que la pide es el
     *  profesor/practicante dueño de esa clase. */
    @Transactional
    public void guardarNota(Long enrollmentId, User owner, String nota) {
        enrollmentRepository.findById(enrollmentId).ifPresent(e -> {
            if (e.getSchedule().getTeacher().getId().equals(owner.getId())) {
                e.setNotasProgreso(recortar(nota, MAX_NOTA));
                enrollmentRepository.save(e);
            }
        });
    }

    public long countEnrollmentsByStudent(User student) {
        return enrollmentRepository.countByStudentAndStatus(student, EnrollmentStatus.ACTIVE);
    }

    /** Cuenta el total de alumnos inscritos en los horarios de un profesor. */
    public long countStudentsOfTeacher(User teacher) {
        long total = 0;
        for (Schedule s : findByTeacher(teacher)) {
            total += countActiveEnrollments(s);
        }
        return total;
    }

    public long countActiveSchedules() {
        return scheduleRepository.count();
    }
}
