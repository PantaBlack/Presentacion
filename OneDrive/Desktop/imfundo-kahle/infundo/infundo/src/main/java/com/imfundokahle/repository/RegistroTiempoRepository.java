package com.imfundokahle.repository;

import com.imfundokahle.model.EstadoTurno;
import com.imfundokahle.model.RegistroTiempo;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Repositorio de turnos de trabajo (RegistroTiempo). */
public interface RegistroTiempoRepository extends JpaRepository<RegistroTiempo, Long> {

    /** El turno activo (en curso o en refrigerio) de un colaborador, si tiene uno. */
    Optional<RegistroTiempo> findFirstByColaboradorAndEstadoInOrderByHoraInicioDesc(User colaborador, List<EstadoTurno> estados);

    /** Todos los turnos activos de todo el mundo: los usa el auto-checkout y el corte de almuerzo. */
    List<RegistroTiempo> findByEstadoIn(List<EstadoTurno> estados);

    /** Turnos de un colaborador en un rango de fechas (para sumar horas de una semana). */
    List<RegistroTiempo> findByColaboradorAndFechaBetween(User colaborador, LocalDate desde, LocalDate hasta);

    List<RegistroTiempo> findByColaboradorOrderByFechaDescHoraInicioDesc(User colaborador);
}
