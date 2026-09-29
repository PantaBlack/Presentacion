package com.imfundokahle.repository;

import com.imfundokahle.model.Language;
import com.imfundokahle.model.Schedule;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Repositorio de horarios. */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByTeacher(User teacher);
    List<Schedule> findByTeacherOrderByDayOfWeekAscStartTimeAsc(User teacher);
    List<Schedule> findByLanguage(Language language);
    List<Schedule> findAllByOrderByDayOfWeekAscStartTimeAsc();
    long countByTeacher(User teacher);
}
