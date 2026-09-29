package com.imfundokahle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "horarios_colaboradores")
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamos este horario directamente con el usuario registrado
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User colaborador;

    @Column(name = "dia_semana", nullable = false, length = 20)
    private String diaSemana; // Ej: "LUNES", "MARTES"

    @Column(name = "hora_inicio", nullable = false, length = 20)
    private String horaInicio; // Ej: "09:00 AM"

    @Column(name = "hora_fin", nullable = false, length = 20)
    private String horaFin; // Ej: "10:00 AM"

    @Column(nullable = false, length = 30)
    private String modalidad; // Ej: "PRESENCIAL", "VIRTUAL", "ALMUERZO", "OCUPADO"

    /** Obligatoria cuando modalidad es "OCUPADO": que va a hacer en esa hora. */
    @Column(length = 500)
    private String actividad;

    public Horario() {}

    public Horario(User colaborador, String diaSemana, String horaInicio, String horaFin, String modalidad) {
        this.colaborador = colaborador;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.modalidad = modalidad;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getColaborador() { return colaborador; }
    public void setColaborador(User colaborador) { this.colaborador = colaborador; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }

    public String getActividad() { return actividad; }
    public void setActividad(String actividad) { this.actividad = actividad; }
}