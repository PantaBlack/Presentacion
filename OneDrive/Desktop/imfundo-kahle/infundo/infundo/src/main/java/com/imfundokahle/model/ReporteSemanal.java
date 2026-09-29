package com.imfundokahle.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reporte semanal de horas de un colaborador: el desglose de tareas por dia
 * que redacta para justificar sus horas cronometradas, revisado y aprobado
 * (o devuelto con comentario) por un administrador.
 * <p>
 * Una vez enviado ({@link EstadoReporte#PENDIENTE_REVISION}) queda bloqueado:
 * el propio colaborador no lo puede editar ni borrar hasta que el admin lo
 * apruebe o lo devuelva a borrador.
 */
@Entity
@Table(name = "reportes_semanales", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "semana_inicio"})
})
public class ReporteSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User colaborador;

    /** Lunes de la semana que cubre este reporte. */
    @Column(name = "semana_inicio", nullable = false)
    private LocalDate semanaInicio;

    @Column(length = 800)
    private String notasLunes;
    @Column(length = 800)
    private String notasMartes;
    @Column(length = 800)
    private String notasMiercoles;
    @Column(length = 800)
    private String notasJueves;
    @Column(length = 800)
    private String notasViernes;

    /** Solicitud de ajuste opcional (edge case: se le olvido marcar y trabajo igual). */
    @Column(name = "minutos_ajuste_solicitado")
    private Integer minutosAjusteSolicitado;

    @Column(name = "comentario_ajuste", length = 500)
    private String comentarioAjuste;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReporte estado = EstadoReporte.BORRADOR;

    /** Comentario del admin al aprobar o al devolver a borrador. */
    @Column(name = "comentario_admin", length = 1000)
    private String comentarioAdmin;

    /** Horas finales que el admin decide que valen para la cuenta del colaborador. */
    @Column(name = "horas_finales_aprobadas")
    private Integer minutosFinalesAprobados;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    public ReporteSemanal() {
    }

    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    /** Si el colaborador todavia puede editar o enviar este reporte. */
    @Transient
    public boolean isEditable() {
        return estado == EstadoReporte.BORRADOR;
    }

    // ---- Getters y Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getColaborador() { return colaborador; }
    public void setColaborador(User colaborador) { this.colaborador = colaborador; }

    public LocalDate getSemanaInicio() { return semanaInicio; }
    public void setSemanaInicio(LocalDate semanaInicio) { this.semanaInicio = semanaInicio; }

    public String getNotasLunes() { return notasLunes; }
    public void setNotasLunes(String notasLunes) { this.notasLunes = notasLunes; }

    public String getNotasMartes() { return notasMartes; }
    public void setNotasMartes(String notasMartes) { this.notasMartes = notasMartes; }

    public String getNotasMiercoles() { return notasMiercoles; }
    public void setNotasMiercoles(String notasMiercoles) { this.notasMiercoles = notasMiercoles; }

    public String getNotasJueves() { return notasJueves; }
    public void setNotasJueves(String notasJueves) { this.notasJueves = notasJueves; }

    public String getNotasViernes() { return notasViernes; }
    public void setNotasViernes(String notasViernes) { this.notasViernes = notasViernes; }

    public Integer getMinutosAjusteSolicitado() { return minutosAjusteSolicitado; }
    public void setMinutosAjusteSolicitado(Integer minutosAjusteSolicitado) { this.minutosAjusteSolicitado = minutosAjusteSolicitado; }

    public String getComentarioAjuste() { return comentarioAjuste; }
    public void setComentarioAjuste(String comentarioAjuste) { this.comentarioAjuste = comentarioAjuste; }

    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }

    public String getComentarioAdmin() { return comentarioAdmin; }
    public void setComentarioAdmin(String comentarioAdmin) { this.comentarioAdmin = comentarioAdmin; }

    public Integer getMinutosFinalesAprobados() { return minutosFinalesAprobados; }
    public void setMinutosFinalesAprobados(Integer minutosFinalesAprobados) { this.minutosFinalesAprobados = minutosFinalesAprobados; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public LocalDateTime getEnviadoEn() { return enviadoEn; }
    public void setEnviadoEn(LocalDateTime enviadoEn) { this.enviadoEn = enviadoEn; }

    public LocalDateTime getRevisadoEn() { return revisadoEn; }
    public void setRevisadoEn(LocalDateTime revisadoEn) { this.revisadoEn = revisadoEn; }
}
