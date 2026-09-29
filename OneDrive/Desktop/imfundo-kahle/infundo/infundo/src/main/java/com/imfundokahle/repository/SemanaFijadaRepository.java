package com.imfundokahle.repository;

import com.imfundokahle.model.SemanaFijada;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SemanaFijadaRepository extends JpaRepository<SemanaFijada, Long> {
    Optional<SemanaFijada> findByColaboradorAndSemanaInicio(User colaborador, LocalDate semanaInicio);
    void deleteByColaboradorAndSemanaInicio(User colaborador, LocalDate semanaInicio);
}
