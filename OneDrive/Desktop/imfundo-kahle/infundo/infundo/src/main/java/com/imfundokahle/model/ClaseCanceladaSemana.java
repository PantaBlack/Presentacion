package com.imfundokahle.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Marca que una clase de horario fijo (Schedule) NO se dicta en una semana
 * puntual (se cancelo, el profesor tiene otra cosa que hacer, etc.), sin
 * borrar el horario ni las inscripciones. Si no existe un registro para una
 * clase y una semana dadas, esa clase se considera activa (verde) por
 * defecto esa semana.
 */
@Entity
@Table(name = "clase_cancelada_semana",
        uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "semana_inicio"}))
public class ClaseCanceladaSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    /** Lunes de la semana a la que aplica esta cancelacion. */
    @Column(name = "semana_inicio", nullable = false)
    private LocalDate semanaInicio;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "registrado_en", updatable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    protected void onCreate() {
        if (registradoEn == null) {
            registradoEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public LocalDate getSemanaInicio() { return semanaInicio; }
    public void setSemanaInicio(LocalDate semanaInicio) { this.semanaInicio = semanaInicio; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getRegistradoEn() { return registradoEn; }
    public void setRegistradoEn(LocalDateTime registradoEn) { this.registradoEn = registradoEn; }
}
