package com.imfundokahle.repository;

import com.imfundokahle.model.Horario;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    
    // Obtener todo el horario de un usuario específico
    List<Horario> findByColaborador(User colaborador);
    
    // Buscar si ya existe una celda específica (para actualizarla en lugar de duplicarla)
    Optional<Horario> findByColaboradorAndDiaSemanaAndHoraInicio(User colaborador, String diaSemana, String horaInicio);
    
    // Eliminar todo el horario de un usuario (útil si deciden resetear su semana)
    void deleteByColaborador(User colaborador);

    // Eliminar una unica celda (al marcarla como "libre" otra vez)
    void deleteByColaboradorAndDiaSemanaAndHoraInicio(User colaborador, String diaSemana, String horaInicio);
}