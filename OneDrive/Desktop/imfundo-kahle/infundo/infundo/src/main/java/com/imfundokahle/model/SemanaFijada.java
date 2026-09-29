package com.imfundokahle.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Marca que un profesor o practicante ya "fijo" su disponibilidad por hora
 * (Horario) de una semana puntual: mientras exista esta fila, la cuadricula
 * de Mi disponibilidad queda bloqueada para esa semana (ver
 * DisponibilidadSemanalService). Si no existe fila para una semana, esa
 * semana todavia se puede editar libremente.
 */
@Entity
@Table(name = "semana_fijada", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "semana_inicio"})
})
public class SemanaFijada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User colaborador;

    @Column(name = "semana_inicio", nullable = false)
    private LocalDate semanaInicio;

    @Column(name = "fijado_en", nullable = false)
    private LocalDateTime fijadoEn;

    public SemanaFijada() {
    }

    @PrePersist
    protected void onCreate() {
        if (fijadoEn == null) {
            fijadoEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getColaborador() { return colaborador; }
    public void setColaborador(User colaborador) { this.colaborador = colaborador; }

    public LocalDate getSemanaInicio() { return semanaInicio; }
    public void setSemanaInicio(LocalDate semanaInicio) { this.semanaInicio = semanaInicio; }

    public LocalDateTime getFijadoEn() { return fijadoEn; }
    public void setFijadoEn(LocalDateTime fijadoEn) { this.fijadoEn = fijadoEn; }
}
