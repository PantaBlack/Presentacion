package com.imfundokahle.service;

import com.imfundokahle.model.EstadoReporte;
import com.imfundokahle.model.RegistroTiempo;
import com.imfundokahle.model.ReporteSemanal;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.RegistroTiempoRepository;
import com.imfundokahle.repository.ReporteSemanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes semanales de horas: el colaborador desglosa sus tareas por dia
 * (mientras esta en BORRADOR), lo envia (queda bloqueado en PENDIENTE_REVISION)
 * y un administrador lo aprueba con las horas finales que decida, o lo devuelve
 * a borrador con un comentario para que se corrija.
 * <p>
 * Las "horas cronometradas por el sistema" nunca se guardan aqui: se calculan
 * siempre a partir de {@link RegistroTiempo}, que es el log de hechos real.
 */
@Service
public class ReporteSemanalService {

    // Mismos limites que las columnas @Column de ReporteSemanal: se recortan aqui
    // ANTES de guardar, en vez de dejar que una fila demasiado larga reviente con
    // una excepcion de Hibernate a medio commit (eso si seria un 500 sin control).
    private static final int LONGITUD_MAXIMA_NOTA = 800;
    private static final int LONGITUD_MAXIMA_COMENTARIO_AJUSTE = 500;
    private static final int LONGITUD_MAXIMA_COMENTARIO_ADMIN = 1000;
    // Tope generoso (una semana completa en minutos) para que nadie pueda mandar
    // por API un numero absurdo (negativo o gigante) como "horas finales".
    private static final int MINUTOS_MAXIMOS_RAZONABLES = 7 * 24 * 60;

    private final ReporteSemanalRepository reporteSemanalRepository;
    private final RegistroTiempoRepository registroTiempoRepository;

    public ReporteSemanalService(ReporteSemanalRepository reporteSemanalRepository,
                                 RegistroTiempoRepository registroTiempoRepository) {
        this.reporteSemanalRepository = reporteSemanalRepository;
        this.registroTiempoRepository = registroTiempoRepository;
    }

    /** El lunes de la semana en la que cae una fecha cualquiera. */
    public static LocalDate lunesDeLaSemana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Plazo maximo para ENVIAR el reporte de una semana: el sabado de esa misma semana, a mediodia. */
    public static LocalDateTime plazoDeEnvio(LocalDate semanaInicio) {
        return semanaInicio.plusDays(5).atTime(12, 0);
    }

    public boolean plazoVencido(LocalDate semanaInicio) {
        return LocalDateTime.now().isAfter(plazoDeEnvio(semanaInicio));
    }

    public ReporteSemanal findById(Long id) {
        return reporteSemanalRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReporteSemanal> findByColaborador(User colaborador) {
        return reporteSemanalRepository.findByColaboradorOrderBySemanaInicioDesc(colaborador);
    }

    @Transactional(readOnly = true)
    public List<ReporteSemanal> findByEstado(EstadoReporte estado) {
        return reporteSemanalRepository.findByEstadoOrderByEnviadoEnAsc(estado);
    }

    @Transactional(readOnly = true)
    public List<ReporteSemanal> findAll() {
        return reporteSemanalRepository.findAllByOrderBySemanaInicioDesc();
    }

    public long countByEstado(EstadoReporte estado) {
        return reporteSemanalRepository.countByEstado(estado);
    }

    /** El borrador de esta semana para el colaborador, o uno nuevo (todavia sin guardar) si no tiene. */
    @Transactional
    public ReporteSemanal obtenerOCrearBorrador(User colaborador, LocalDate semanaInicio) {
        return reporteSemanalRepository.findByColaboradorAndSemanaInicio(colaborador, semanaInicio)
                .orElseGet(() -> {
                    ReporteSemanal r = new ReporteSemanal();
                    r.setColaborador(colaborador);
                    r.setSemanaInicio(semanaInicio);
                    return r;
                });
    }

    /**
     * Guarda el desglose de la semana. Si el reporte ya existe y no esta en
     * BORRADOR (ya fue enviado o aprobado), no se toca: un reporte enviado
     * queda bloqueado para su dueno hasta que el admin lo apruebe o lo observe.
     */
    @Transactional
    public ReporteSemanal guardarBorrador(User colaborador, LocalDate semanaInicio,
                                          String lunes, String martes, String miercoles,
                                          String jueves, String viernes,
                                          Integer minutosAjuste, String comentarioAjuste) {
        ReporteSemanal r = obtenerOCrearBorrador(colaborador, semanaInicio);
        if (r.getId() != null && !r.isEditable()) {
            return r;
        }
        r.setNotasLunes(truncar(lunes, LONGITUD_MAXIMA_NOTA));
        r.setNotasMartes(truncar(martes, LONGITUD_MAXIMA_NOTA));
        r.setNotasMiercoles(truncar(miercoles, LONGITUD_MAXIMA_NOTA));
        r.setNotasJueves(truncar(jueves, LONGITUD_MAXIMA_NOTA));
        r.setNotasViernes(truncar(viernes, LONGITUD_MAXIMA_NOTA));
        r.setMinutosAjusteSolicitado(acotarMinutos(minutosAjuste));
        r.setComentarioAjuste(truncar(comentarioAjuste, LONGITUD_MAXIMA_COMENTARIO_AJUSTE));
        return reporteSemanalRepository.save(r);
    }

    /**
     * Envia el reporte a revision: a partir de aqui el dueno no lo puede editar
     * ni borrar. Devuelve "ok", "vencido" (paso el plazo del sabado a mediodia
     * de esa semana) o "error" (no existe, no es suyo, o ya no esta en borrador).
     */
    @Transactional
    public String enviar(Long id, User colaborador) {
        ReporteSemanal r = findById(id);
        if (r == null || !r.getColaborador().getId().equals(colaborador.getId()) || !r.isEditable()) {
            return "error";
        }
        if (plazoVencido(r.getSemanaInicio())) {
            return "vencido";
        }
        r.setEstado(EstadoReporte.PENDIENTE_REVISION);
        r.setEnviadoEn(LocalDateTime.now());
        reporteSemanalRepository.save(r);
        return "ok";
    }

    /**
     * El admin aprueba con las horas finales que decida (pueden diferir de lo
     * cronometrado o lo pedido). Devuelve false si el reporte no existe o si
     * no estaba pendiente de revision (no tiene sentido "aprobar" un borrador).
     */
    @Transactional
    public boolean aprobar(Long id, int minutosFinales, String comentarioAdmin) {
        ReporteSemanal r = findById(id);
        if (r == null || r.getEstado() != EstadoReporte.PENDIENTE_REVISION) {
            return false;
        }
        r.setMinutosFinalesAprobados(acotarMinutos(minutosFinales));
        r.setComentarioAdmin(truncar(comentarioAdmin, LONGITUD_MAXIMA_COMENTARIO_ADMIN));
        r.setEstado(EstadoReporte.APROBADO);
        r.setRevisadoEn(LocalDateTime.now());
        reporteSemanalRepository.save(r);
        return true;
    }

    /**
     * El admin lo devuelve a borrador con un comentario, para que el colaborador
     * lo corrija y reenvie. Devuelve false si el reporte no existe, no estaba
     * pendiente de revision, o el comentario viene vacio (sin explicacion no
     * hay nada que corregir).
     */
    @Transactional
    public boolean observar(Long id, String comentarioAdmin) {
        ReporteSemanal r = findById(id);
        String comentario = truncar(comentarioAdmin, LONGITUD_MAXIMA_COMENTARIO_ADMIN);
        if (r == null || r.getEstado() != EstadoReporte.PENDIENTE_REVISION || comentario == null) {
            return false;
        }
        r.setComentarioAdmin(comentario);
        r.setEstado(EstadoReporte.BORRADOR);
        r.setRevisadoEn(LocalDateTime.now());
        reporteSemanalRepository.save(r);
        return true;
    }

    /**
     * Agrega (sin borrar lo que ya haya) una linea al reporte del dia indicado, para que
     * lo que un profesor/practicante ya explico al cancelar una clase esa semana ("Cancele
     * porque...") no lo tenga que volver a escribir a mano en su reporte semanal. Si el
     * reporte de esa semana ya fue enviado o aprobado, no se toca (queda bloqueado para su
     * dueno igual que cualquier otra edicion). Si el mismo texto ya esta anotado ese dia
     * (ej. cancelar y reactivar la misma clase varias veces), no lo duplica.
     */
    @Transactional
    public void agregarNotaAutomatica(User colaborador, LocalDate semanaInicio, String dia, String texto) {
        String limpio = texto == null ? null : texto.strip();
        if (limpio == null || limpio.isEmpty()) {
            return;
        }
        ReporteSemanal r = obtenerOCrearBorrador(colaborador, semanaInicio);
        if (r.getId() != null && !r.isEditable()) {
            return;
        }
        String actual = notaDelDia(r, dia);
        if (actual != null && actual.contains(limpio)) {
            return;
        }
        String combinado = (actual == null || actual.isBlank()) ? limpio : actual + "; " + limpio;
        establecerNotaDelDia(r, dia, truncar(combinado, LONGITUD_MAXIMA_NOTA));
        reporteSemanalRepository.save(r);
    }

    private static String notaDelDia(ReporteSemanal r, String dia) {
        return switch (dia) {
            case "LUNES" -> r.getNotasLunes();
            case "MARTES" -> r.getNotasMartes();
            case "MIERCOLES" -> r.getNotasMiercoles();
            case "JUEVES" -> r.getNotasJueves();
            case "VIERNES" -> r.getNotasViernes();
            default -> null;
        };
    }

    private static void establecerNotaDelDia(ReporteSemanal r, String dia, String valor) {
        switch (dia) {
            case "LUNES" -> r.setNotasLunes(valor);
            case "MARTES" -> r.setNotasMartes(valor);
            case "MIERCOLES" -> r.setNotasMiercoles(valor);
            case "JUEVES" -> r.setNotasJueves(valor);
            case "VIERNES" -> r.setNotasViernes(valor);
            default -> { }
        }
    }

    /** Recorta espacios y longitud; una cadena vacia (o solo espacios) se guarda como null. */
    private static String truncar(String texto, int longitudMaxima) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        if (limpio.isEmpty()) {
            return null;
        }
        return limpio.length() > longitudMaxima ? limpio.substring(0, longitudMaxima) : limpio;
    }

    /** Nunca negativo ni por encima del tope razonable, sin importar lo que llegue por parametro. */
    private static Integer acotarMinutos(Integer minutos) {
        if (minutos == null) {
            return null;
        }
        return Math.max(0, Math.min(minutos, MINUTOS_MAXIMOS_RAZONABLES));
    }

    private static int acotarMinutos(int minutos) {
        return Math.max(0, Math.min(minutos, MINUTOS_MAXIMOS_RAZONABLES));
    }

    /** Minutos cronometrados por el sistema para esa semana, dia por dia (LUNES..VIERNES). */
    @Transactional(readOnly = true)
    public Map<String, Long> minutosCronometradosPorDia(User colaborador, LocalDate semanaInicio) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (String dia : HorarioService.DIAS_SEMANA) {
            mapa.put(dia, 0L);
        }
        List<RegistroTiempo> turnos = registroTiempoRepository
                .findByColaboradorAndFechaBetween(colaborador, semanaInicio, semanaInicio.plusDays(4));
        for (RegistroTiempo t : turnos) {
            String dia = HorarioService.nombreDia(t.getFecha());
            if (dia != null) {
                mapa.merge(dia, t.getMinutosTrabajados(), Long::sum);
            }
        }
        return mapa;
    }

    @Transactional(readOnly = true)
    public long minutosCronometradosSemana(User colaborador, LocalDate semanaInicio) {
        return minutosCronometradosPorDia(colaborador, semanaInicio).values()
                .stream().mapToLong(Long::longValue).sum();
    }
}
