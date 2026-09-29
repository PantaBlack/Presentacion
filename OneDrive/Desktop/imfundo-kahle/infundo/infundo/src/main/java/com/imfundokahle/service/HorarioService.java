package com.imfundokahle.service;

import com.imfundokahle.model.Horario;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.HorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HorarioService {

    /** Modalidad que representa una celda vacia: no se guarda fila, se borra si existia. */
    public static final String LIBRE = "LIBRE";

    /** Franja horaria del centro: 08:00 a 20:00, en bloques de una hora. */
    public static final List<String> HORAS_DEL_DIA = List.of(
            "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00",
            "15:00", "16:00", "17:00", "18:00", "19:00"
    );

    /** Dias habiles del centro, en el orden en que se muestran. */
    public static final List<String> DIAS_SEMANA = List.of(
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"
    );

    /**
     * Modalidades que la persona puede elegir a mano para una celda, en el orden en que
     * se van alternando al hacer clic. "ALMUERZO" no esta aqui a proposito: esa hora ya
     * esta fija a las 13:00 para todo el mundo (ver HORA_ALMUERZO_OBLIGATORIA / MODALIDAD_ALMUERZO
     * mas abajo), asi que ofrecerla como opcion en cualquier otra hora no tendria sentido.
     */
    public static final List<String> CICLO_MODALIDADES = List.of(LIBRE, "PRESENCIAL", "VIRTUAL", "OCUPADO");

    /** Ocupado con otra actividad (rojo): la unica modalidad que exige describir que se va a hacer. */
    public static final String MODALIDAD_OCUPADO = "OCUPADO";

    private static final int MAX_ACTIVIDAD = 500;

    /**
     * Hora de almuerzo obligatoria para todo el mundo, todos los dias: nadie
     * la puede editar ni el admin la puede usar para asignar una tarea.
     * Se aplica siempre en cada lectura, sin importar lo que haya guardado
     * en la base de datos.
     */
    public static final String HORA_ALMUERZO_OBLIGATORIA = "13:00";
    public static final String MODALIDAD_ALMUERZO = "ALMUERZO";

    private static final Map<DayOfWeek, String> DIA_A_STRING = Map.of(
            DayOfWeek.MONDAY, "LUNES",
            DayOfWeek.TUESDAY, "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY, "JUEVES",
            DayOfWeek.FRIDAY, "VIERNES"
    );

    /** Nombre de dia (LUNES..VIERNES) para una fecha, o null si cae en fin de semana. */
    public static String nombreDia(LocalDate fecha) {
        return DIA_A_STRING.get(fecha.getDayOfWeek());
    }

    private final HorarioRepository horarioRepository;

    public HorarioService(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Horario> obtenerHorarioPorColaborador(User colaborador) {
        return horarioRepository.findByColaborador(colaborador);
    }

    /**
     * El horario real de un colaborador, pero con la hora de almuerzo obligatoria
     * ya forzada en los 5 dias (reemplazando cualquier otra cosa que hubiera ahi).
     * Este es el horario "verdadero" que debe verse en cualquier pantalla: la
     * cuadricula propia, la del admin al asignar tareas, y las vistas de inspeccion.
     */
    @Transactional(readOnly = true)
    public List<Horario> obtenerHorarioEfectivo(User colaborador) {
        List<Horario> efectivo = new ArrayList<>();
        for (Horario h : obtenerHorarioPorColaborador(colaborador)) {
            if (!HORA_ALMUERZO_OBLIGATORIA.equals(h.getHoraInicio())) {
                efectivo.add(h);
            }
        }
        for (String dia : DIAS_SEMANA) {
            efectivo.add(new Horario(colaborador, dia, HORA_ALMUERZO_OBLIGATORIA,
                    siguienteHora(HORA_ALMUERZO_OBLIGATORIA), MODALIDAD_ALMUERZO));
        }
        return efectivo;
    }

    /**
     * Disponibilidad de un colaborador como mapa dia -> hora -> modalidad,
     * listo para pintar directamente en una cuadricula (sin buscar en listas
     * dentro de la plantilla). Las celdas sin dato no aparecen en el mapa,
     * salvo la hora de almuerzo obligatoria, que siempre aparece.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> obtenerMapaSemanal(User colaborador) {
        Map<String, Map<String, String>> mapa = new LinkedHashMap<>();
        for (String dia : DIAS_SEMANA) {
            mapa.put(dia, new LinkedHashMap<>());
        }
        for (Horario h : obtenerHorarioEfectivo(colaborador)) {
            mapa.computeIfAbsent(h.getDiaSemana(), d -> new LinkedHashMap<>())
                    .put(h.getHoraInicio(), h.getModalidad());
        }
        return mapa;
    }

    @Transactional
    public Horario guardarOActualizarCelda(User colaborador, String diaSemana, String horaInicio, String horaFin,
                                           String modalidad, String actividad) {
        // Buscar si el usuario ya tiene una celda registrada en ese día y a esa hora
        Optional<Horario> celdaExistente = horarioRepository.findByColaboradorAndDiaSemanaAndHoraInicio(colaborador, diaSemana, horaInicio);

        Horario horario = celdaExistente.orElseGet(() -> new Horario(colaborador, diaSemana, horaInicio, horaFin, modalidad));
        horario.setModalidad(modalidad);
        horario.setActividad(actividad);
        return horarioRepository.save(horario);
    }

    /**
     * Actualiza una celda de la propia disponibilidad: si la modalidad es LIBRE
     * borra la celda (para no dejar filas vacias en la base de datos), y si no,
     * la crea o actualiza cubriendo exactamente una hora. Si la modalidad es
     * OCUPADO, la actividad es obligatoria (sin ella no se guarda nada); para
     * cualquier otra modalidad, la actividad se ignora (se guarda null).
     * <p>
     * La hora de almuerzo obligatoria nunca se puede editar (ni desde la
     * cuadricula propia ni con una peticion armada a mano): esta linea es la
     * que de verdad protege la regla, la del frontend es solo comodidad visual.
     *
     * @return true si el cambio se aplico; false si se rechazo (almuerzo obligatorio,
     *         o OCUPADO sin actividad).
     */
    @Transactional
    public boolean marcarCelda(User colaborador, String diaSemana, String horaInicio, String modalidad, String actividad) {
        if (HORA_ALMUERZO_OBLIGATORIA.equals(horaInicio)) {
            return false;
        }
        if (LIBRE.equals(modalidad)) {
            horarioRepository.deleteByColaboradorAndDiaSemanaAndHoraInicio(colaborador, diaSemana, horaInicio);
            return true;
        }
        String actividadFinal = null;
        if (MODALIDAD_OCUPADO.equals(modalidad)) {
            if (actividad == null || actividad.isBlank()) {
                return false;
            }
            String limpia = actividad.strip();
            actividadFinal = limpia.length() > MAX_ACTIVIDAD ? limpia.substring(0, MAX_ACTIVIDAD) : limpia;
        }
        String horaFin = siguienteHora(horaInicio);
        guardarOActualizarCelda(colaborador, diaSemana, horaInicio, horaFin, modalidad, actividadFinal);
        return true;
    }

    private String siguienteHora(String horaInicio) {
        return java.time.LocalTime.parse(horaInicio).plusHours(1).toString();
    }

    @Transactional
    public void resetearHorario(User colaborador) {
        horarioRepository.deleteByColaborador(colaborador);
    }
}
