package com.imfundokahle.repository;

import com.imfundokahle.model.EstadoReporte;
import com.imfundokahle.model.ReporteSemanal;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Repositorio de reportes semanales de horas. */
public interface ReporteSemanalRepository extends JpaRepository<ReporteSemanal, Long> {

    Optional<ReporteSemanal> findByColaboradorAndSemanaInicio(User colaborador, LocalDate semanaInicio);

    List<ReporteSemanal> findByColaboradorOrderBySemanaInicioDesc(User colaborador);

    List<ReporteSemanal> findByEstadoOrderByEnviadoEnAsc(EstadoReporte estado);

    List<ReporteSemanal> findAllByOrderBySemanaInicioDesc();

    long countByEstado(EstadoReporte estado);
}
