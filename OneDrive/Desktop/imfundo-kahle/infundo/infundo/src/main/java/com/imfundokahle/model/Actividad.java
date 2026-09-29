package com.imfundokahle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividades_control")
public class Actividad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_asignado", nullable = false, length = 150)
    private String nombreAsignado;

    /**
     * A quien pertenece esta actividad, por id de usuario (no por nombre): dos personas
     * con el mismo nombre (o mayusculas distintas) antes se mezclaban entre si porque el
     * cruce se hacia por texto. "nombreAsignado" se sigue guardando solo para mostrarlo
     * en la tabla del admin; el cruce real para "mis actividades" usa este id.
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 50)
    private String rol; // Ej: Practicante, Profesor

    /** Dia de la semana tomado de la disponibilidad de la persona (LUNES..VIERNES). Opcional. */
    @Column(name = "dia_semana", length = 20)
    private String diaSemana;

    @Column(nullable = false, length = 255)
    private String tareaDetalle;

    @Column(name = "horas_planificadas", nullable = false)
    private Integer horasPlanificadas;

    @Column(name = "rango_horario", nullable = false, length = 100)
    private String rangoHorario;

    @Column(nullable = false, length = 50)
    private String estadoActual; 

    @Column(length = 500)
    private String notasAdicionales;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // Métodos de ciclo de vida de JPA para auditoría
    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
        if (this.estadoActual == null || this.estadoActual.trim().isEmpty()) {
            this.estadoActual = "Pendiente";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }

    // Constructor vacío requerido por JPA
    public Actividad() {}

    // Constructor con parámetros
    public Actividad(String nombreAsignado, String rol, String tareaDetalle, Integer horasPlanificadas, String rangoHorario, String estadoActual, String notasAdicionales) {
        this.nombreAsignado = nombreAsignado;
        this.rol = rol;
        this.tareaDetalle = tareaDetalle;
        this.horasPlanificadas = horasPlanificadas;
        this.rangoHorario = rangoHorario;
        this.estadoActual = estadoActual;
        this.notasAdicionales = notasAdicionales;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreAsignado() { return nombreAsignado; }
    public void setNombreAsignado(String nombreAsignado) { this.nombreAsignado = nombreAsignado; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public String getTareaDetalle() { return tareaDetalle; }
    public void setTareaDetalle(String tareaDetalle) { this.tareaDetalle = tareaDetalle; }

    public Integer getHorasPlanificadas() { return horasPlanificadas; }
    public void setHorasPlanificadas(Integer horasPlanificadas) { this.horasPlanificadas = horasPlanificadas; }

    public String getRangoHorario() { return rangoHorario; }
    public void setRangoHorario(String rangoHorario) { this.rangoHorario = rangoHorario; }

    public String getEstadoActual() { return estadoActual; }
    public void setEstadoActual(String estadoActual) { this.estadoActual = estadoActual; }

    public String getNotasAdicionales() { return notasAdicionales; }
    public void setNotasAdicionales(String notasAdicionales) { this.notasAdicionales = notasAdicionales; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}