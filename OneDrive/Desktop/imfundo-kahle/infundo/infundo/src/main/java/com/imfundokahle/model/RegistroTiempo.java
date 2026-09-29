package com.imfundokahle.model;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un turno de trabajo de un colaborador, medido por timestamps del servidor
 * (nunca por un cronometro del navegador, que se pierde con una caida de
 * internet o un cierre de pestana).
 * <p>
 * Es un log de hechos: una vez que el turno queda FINALIZADO no se vuelve a
 * tocar. Si algo salio mal (el colaborador olvido reanudar tras el almuerzo,
 * por ejemplo), la correccion se pide y se resuelve en el {@link ReporteSemanal}
 * de esa semana, no reescribiendo este registro.
 */
@Entity
@Table(name = "registros_tiempo")
public class RegistroTiempo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User colaborador;

    /** Dia calendario en que empezo el turno (ancla para turnos que cruzan la medianoche). */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    /** La pone el job automatico de las 13:00, nunca el usuario. */
    @Column(name = "inicio_almuerzo")
    private LocalDateTime inicioAlmuerzo;

    /** La pone el usuario al tocar "Reanudar turno": es estrictamente manual. */
    @Column(name = "fin_almuerzo")
    private LocalDateTime finAlmuerzo;

    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTurno estado = EstadoTurno.EN_TURNO;

    /** True si el auto-checkout lo cerro porque el colaborador olvido marcar salida. */
    @Column(name = "cierre_forzado", nullable = false)
    private boolean cierreForzado = false;

    public RegistroTiempo() {
    }

    public RegistroTiempo(User colaborador, LocalDateTime horaInicio) {
        this.colaborador = colaborador;
        this.horaInicio = horaInicio;
        this.fecha = horaInicio.toLocalDate();
    }

    /**
     * Minutos realmente trabajados hasta ahora (o hasta el cierre, si ya cerro).
     * Si el almuerzo empezo pero nunca se reanudo, el conteo se detiene ahi:
     * la tarde sin marcar nunca se cuenta como trabajada.
     */
    @Transient
    public long getMinutosTrabajados() {
        LocalDateTime fin = (horaFin != null) ? horaFin : LocalDateTime.now();
        if (inicioAlmuerzo != null && finAlmuerzo == null) {
            fin = inicioAlmuerzo;
        }
        long totalMinutos = Duration.between(horaInicio, fin).toMinutes();
        if (inicioAlmuerzo != null && finAlmuerzo != null) {
            totalMinutos -= Duration.between(inicioAlmuerzo, finAlmuerzo).toMinutes();
        }
        return Math.max(0, totalMinutos);
    }

    @Transient
    public String getMinutosTrabajadosFormateado() {
        return formatoMinutos(getMinutosTrabajados());
    }

    /** Da formato "Xh Ym" a una cantidad de minutos, para mostrar en pantalla. */
    public static String formatoMinutos(long totalMinutos) {
        long horas = totalMinutos / 60;
        long minutos = totalMinutos % 60;
        return horas + "h " + minutos + "m";
    }

    // ---- Getters y Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getColaborador() { return colaborador; }
    public void setColaborador(User colaborador) { this.colaborador = colaborador; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDateTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalDateTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalDateTime getInicioAlmuerzo() { return inicioAlmuerzo; }
    public void setInicioAlmuerzo(LocalDateTime inicioAlmuerzo) { this.inicioAlmuerzo = inicioAlmuerzo; }

    public LocalDateTime getFinAlmuerzo() { return finAlmuerzo; }
    public void setFinAlmuerzo(LocalDateTime finAlmuerzo) { this.finAlmuerzo = finAlmuerzo; }

    public LocalDateTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalDateTime horaFin) { this.horaFin = horaFin; }

    public EstadoTurno getEstado() { return estado; }
    public void setEstado(EstadoTurno estado) { this.estado = estado; }

    public boolean isCierreForzado() { return cierreForzado; }
    public void setCierreForzado(boolean cierreForzado) { this.cierreForzado = cierreForzado; }
}
