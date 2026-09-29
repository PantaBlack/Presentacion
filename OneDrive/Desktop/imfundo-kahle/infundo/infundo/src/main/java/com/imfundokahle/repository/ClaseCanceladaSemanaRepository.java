package com.imfundokahle.repository;

import com.imfundokahle.model.ClaseCanceladaSemana;
import com.imfundokahle.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClaseCanceladaSemanaRepository extends JpaRepository<ClaseCanceladaSemana, Long> {
    Optional<ClaseCanceladaSemana> findByScheduleAndSemanaInicio(Schedule schedule, LocalDate semanaInicio);

    List<ClaseCanceladaSemana> findBySemanaInicio(LocalDate semanaInicio);

    void deleteByScheduleAndSemanaInicio(Schedule schedule, LocalDate semanaInicio);
}
