package com.imfundokahle.config;

import com.imfundokahle.model.*;
import com.imfundokahle.repository.RegistroTiempoRepository;
import com.imfundokahle.repository.ReporteSemanalRepository;
import com.imfundokahle.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Siembra datos de demostracion al arrancar con la base de datos vacia:
 * un administrador, practicantes, profesores y alumnos, con su disponibilidad
 * semanal, algunos cursos con inscripciones, y un par de propuestas/materiales.
 * <p>
 * Las credenciales no estan fijas en el codigo: se leen de application.properties,
 * que a su vez las toma de variables de entorno (ADMIN_PASSWORD, SEED_PASSWORD).
 * La base H2 es en memoria y se recrea en cada arranque (ddl-auto=create-drop),
 * por eso el valor por defecto es fijo en vez de generarse al azar cada vez:
 * de lo contrario habria que leer el log en cada reinicio solo para poder entrar.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Una persona a sembrar: nombre, apellido y correo ya definidos explicitamente. */
    private record Seed(String firstName, String lastName, String email) {
    }

    private static final List<Seed> PRACTICANTES = List.of(
            new Seed("Alvaro", "Roca", "alvaro.roca@imfundo.org"),
            new Seed("Claudia", "Barcelli", "claudia.barcelli@imfundo.org"),
            new Seed("Anderson", "Larios", "anderson.larios@imfundo.org"),
            new Seed("Briana", "Montoya", "briana.montoya@imfundo.org"),
            new Seed("Gianino", "Naranjo", "gianino.naranjo@imfundo.org"),
            new Seed("Yimmy", "Diaz", "yimmy.diaz@imfundo.org"),
            new Seed("Luis", "Mendoza", "luis.mendoza@imfundo.org"),
            new Seed("Camila", "Vargas", "camila.vargas@imfundo.org"),
            new Seed("Diego", "Fernandez", "diego.fernandez@imfundo.org"),
            new Seed("Valeria", "Castillo", "valeria.castillo@imfundo.org"),
            new Seed("Mateo", "Suarez", "mateo.suarez@imfundo.org"),
            new Seed("Sofia", "Rojas", "sofia.rojas@imfundo.org"),
            new Seed("Joaquin", "Silva", "joaquin.silva@imfundo.org")
    );

    private static final List<Seed> PROFESORES = List.of(
            new Seed("Maria", "Gonzalez", "maria.gonzalez@imfundo.org"),
            new Seed("Jean", "Dupont", "jean.dupont@imfundo.org"),
            new Seed("Roberto", "Gomez", "roberto.gomez@imfundo.org"),
            new Seed("Ana", "Villanueva", "ana.villanueva@imfundo.org"),
            new Seed("Pedro", "Pascal", "pedro.pascal@imfundo.org"),
            new Seed("Carmen", "Salinas", "carmen.salinas@imfundo.org"),
            new Seed("Miguel", "Trauco", "miguel.trauco@imfundo.org"),
            new Seed("Rosa", "Dominguez", "rosa.dominguez@imfundo.org"),
            new Seed("Jorge", "Chavez", "jorge.chavez@imfundo.org"),
            new Seed("Elena", "Ramos", "elena.ramos@imfundo.org"),
            new Seed("Fernando", "Alonso", "fernando.alonso@imfundo.org"),
            new Seed("Patricia", "Santos", "patricia.santos@imfundo.org"),
            new Seed("Ricardo", "Palma", "ricardo.palma@imfundo.org"),
            new Seed("Monica", "Galindo", "monica.galindo@imfundo.org"),
            new Seed("Hugo", "Sanchez", "hugo.sanchez@imfundo.org"),
            new Seed("Teresa", "Cruz", "teresa.cruz@imfundo.org"),
            new Seed("Victor", "Caceres", "victor.caceres@imfundo.org"),
            new Seed("Blanca", "Varela", "blanca.varela@imfundo.org"),
            new Seed("Arturo", "Vidal", "arturo.vidal@imfundo.org"),
            new Seed("Natalia", "Malaga", "natalia.malaga@imfundo.org")
    );

    private static final List<Seed> ALUMNOS = List.of(
            new Seed("Lucia", "Ramirez", "lucia.ramirez@imfundo.org"),
            new Seed("Carlos", "Torres", "carlos.torres@imfundo.org"),
            new Seed("Andrea", "Luna", "andrea.luna@imfundo.org"),
            new Seed("Sebastian", "Arce", "sebastian.arce@imfundo.org"),
            new Seed("Daniela", "Vega", "daniela.vega@imfundo.org"),
            new Seed("Alejandro", "Gomez", "alejandro.gomez@imfundo.org"),
            new Seed("Victoria", "Paz", "victoria.paz@imfundo.org"),
            new Seed("Gabriel", "Flores", "gabriel.flores@imfundo.org"),
            new Seed("Gabriela", "Mendez", "gabriela.mendez@imfundo.org"),
            new Seed("Tomas", "Aquino", "tomas.aquino@imfundo.org"),
            new Seed("Mariana", "Costa", "mariana.costa@imfundo.org"),
            new Seed("Nicolas", "Peña", "nicolas.pena@imfundo.org"),
            new Seed("Valentina", "Castro", "valentina.castro@imfundo.org"),
            new Seed("Rodrigo", "Aliaga", "rodrigo.aliaga@imfundo.org"),
            new Seed("Ximena", "Morales", "ximena.morales@imfundo.org"),
            new Seed("Alonso", "Reyes", "alonso.reyes@imfundo.org"),
            new Seed("Renata", "Pineda", "renata.pineda@imfundo.org"),
            new Seed("Eduardo", "Blanco", "eduardo.blanco@imfundo.org"),
            new Seed("Antonella", "Rios", "antonella.rios@imfundo.org"),
            new Seed("Leonardo", "Ortiz", "leonardo.ortiz@imfundo.org"),
            new Seed("Fatima", "Cruz", "fatima.cruz@imfundo.org"),
            new Seed("Mauricio", "Soto", "mauricio.soto@imfundo.org"),
            new Seed("Carolina", "Herrera", "carolina.herrera@imfundo.org"),
            new Seed("Simon", "Bolivar", "simon.bolivar@imfundo.org"),
            new Seed("Juliana", "Osorio", "juliana.osorio@imfundo.org"),
            new Seed("Benjamin", "Franco", "benjamin.franco@imfundo.org"),
            new Seed("Paula", "Medina", "paula.medina@imfundo.org"),
            new Seed("Lucas", "Navarro", "lucas.navarro@imfundo.org"),
            new Seed("Romina", "Paredes", "romina.paredes@imfundo.org"),
            new Seed("Emilio", "Carrillo", "emilio.carrillo@imfundo.org"),
            new Seed("Luciana", "Vidal", "luciana.vidal@imfundo.org"),
            new Seed("Hector", "Lavoe", "hector.lavoe@imfundo.org"),
            new Seed("Samantha", "Campos", "samantha.campos@imfundo.org"),
            new Seed("Andres", "Wiese", "andres.wiese@imfundo.org"),
            new Seed("Melissa", "Loza", "melissa.loza@imfundo.org")
    );

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final ProposalService proposalService;
    private final MaterialService materialService;
    private final HorarioService horarioService;
    private final RegistroTiempoRepository registroTiempoRepository;
    private final ReporteSemanalRepository reporteSemanalRepository;
    private final DisponibilidadSemanalService disponibilidadSemanalService;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.demo-password}")
    private String demoPassword;

    @Value("${app.seed.demo-users-enabled:true}")
    private boolean demoUsersEnabled;

    public DataInitializer(UserService userService, ScheduleService scheduleService,
                           ProposalService proposalService, MaterialService materialService,
                           HorarioService horarioService, RegistroTiempoRepository registroTiempoRepository,
                           ReporteSemanalRepository reporteSemanalRepository,
                           DisponibilidadSemanalService disponibilidadSemanalService) {
        this.disponibilidadSemanalService = disponibilidadSemanalService;
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.proposalService = proposalService;
        this.materialService = materialService;
        this.horarioService = horarioService;
        this.registroTiempoRepository = registroTiempoRepository;
        this.reporteSemanalRepository = reporteSemanalRepository;
    }

    @Override
    public void run(String... args) {
        if (userService.countAll() > 0) {
            return; // Ya se sembro antes: no duplicar datos.
        }

        sembrarAdmin();
        if (!demoUsersEnabled) {
            return;
        }

        List<User> practicantes = crearUsuarios(PRACTICANTES, Role.PRACTICANTE);
        List<User> profesores = crearUsuarios(PROFESORES, Role.TEACHER);
        List<User> alumnos = crearUsuarios(ALUMNOS, Role.STUDENT);

        sembrarDisponibilidadPracticantes(practicantes);
        sembrarDisponibilidadProfesores(profesores);
        sembrarDisponibilidadAlumnos(alumnos);
        sembrarCursosEInscripciones(profesores, alumnos);
        sembrarPropuestasYMateriales(profesores);
        sembrarControlDeTiempo(practicantes, profesores);
        sembrarEstadoSemanalHoras(profesores, practicantes);
    }

    /**
     * Marca un par de horas como "OCUPADO" (rojo, con actividad) en la cuadricula por hora
     * de un profesor y un practicante, y fija esa semana: asi se puede ver de entrada como
     * se ve una semana ya fijada (bloqueada, con la actividad copiada al reporte semanal),
     * sin tener que marcarla y fijarla a mano primero. Se evita profesores.get(0)/get(2) y
     * practicantes.get(0): ya tienen un reporte semanal de la semana actual sembrado en
     * sembrarControlDeTiempo, y mezclar ambos aqui solo ensuciaria la demo.
     */
    private void sembrarEstadoSemanalHoras(List<User> profesores, List<User> practicantes) {
        LocalDate semanaActual = disponibilidadSemanalService.inicioDeSemanaActual();

        User profesor = profesores.get(3);
        horarioService.marcarCelda(profesor, "MARTES", "10:00", "OCUPADO",
                "Reunion de coordinacion pedagogica con el area academica");
        horarioService.marcarCelda(profesor, "JUEVES", "15:00", "OCUPADO",
                "Capacitacion sobre la nueva plataforma de examenes");
        disponibilidadSemanalService.fijarSemana(profesor, semanaActual, horarioService.obtenerHorarioEfectivo(profesor));

        User practicante = practicantes.get(2);
        horarioService.marcarCelda(practicante, "VIERNES", "10:00", "OCUPADO",
                "Apoyo en la matricula de alumnos nuevos");
        horarioService.marcarCelda(practicante, "VIERNES", "15:00", "OCUPADO",
                "Entrega del reporte mensual de practicas");
        disponibilidadSemanalService.fijarSemana(practicante, semanaActual, horarioService.obtenerHorarioEfectivo(practicante));
    }

    /** Cuenta administradora inicial (unica, definida por configuracion). */
    private void sembrarAdmin() {
        userService.createUser("Admin", "Viva Idiomas", adminEmail, adminPassword, Role.ADMIN);
        log.warn("=================================================================");
        log.warn(" Cuenta ADMIN lista.  Correo: {}", adminEmail);
        log.warn(" Define ADMIN_PASSWORD en el entorno antes de desplegar en un");
        log.warn(" servidor real: la contrasena por defecto NO es segura ahi.");
        log.warn("=================================================================");
    }

    /** Crea todas las personas de una lista con el mismo rol y la misma contrasena de demo. */
    private List<User> crearUsuarios(List<Seed> personas, Role role) {
        return personas.stream()
                .map(p -> userService.createUser(p.firstName(), p.lastName(), p.email(), demoPassword, role))
                .toList();
    }

    // ====================================================================================
    // Disponibilidad semanal (celda a celda, en bloques de una hora)
    // ====================================================================================

    private void sembrarDisponibilidadPracticantes(List<User> practicantes) {
        // Equipo de manana
        for (User p : practicantes.subList(0, 4)) {
            crearDisponibilidad(p, "LUNES", "09:00", "13:00", "VIRTUAL");
            crearDisponibilidad(p, "LUNES", "13:00", "14:00", "ALMUERZO");
            crearDisponibilidad(p, "MARTES", "09:00", "13:00", "PRESENCIAL");
            crearDisponibilidad(p, "MIERCOLES", "14:00", "18:00", "VIRTUAL");
            crearDisponibilidad(p, "JUEVES", "09:00", "13:00", "PRESENCIAL");
        }
        // Equipo de tarde
        for (User p : practicantes.subList(4, 9)) {
            crearDisponibilidad(p, "LUNES", "14:00", "18:00", "PRESENCIAL");
            crearDisponibilidad(p, "MARTES", "13:00", "14:00", "ALMUERZO");
            crearDisponibilidad(p, "MARTES", "14:00", "18:00", "VIRTUAL");
            crearDisponibilidad(p, "JUEVES", "14:00", "18:00", "PRESENCIAL");
            crearDisponibilidad(p, "VIERNES", "09:00", "13:00", "VIRTUAL");
        }
        // Equipo mixto
        for (User p : practicantes.subList(9, 13)) {
            crearDisponibilidad(p, "MIERCOLES", "09:00", "13:00", "PRESENCIAL");
            crearDisponibilidad(p, "MIERCOLES", "13:00", "14:00", "ALMUERZO");
            crearDisponibilidad(p, "VIERNES", "14:00", "18:00", "VIRTUAL");
        }
    }

    private void sembrarDisponibilidadProfesores(List<User> profesores) {
        for (User pr : profesores.subList(0, 7)) {
            crearDisponibilidad(pr, "LUNES", "08:00", "12:00", "VIRTUAL");
            crearDisponibilidad(pr, "MIERCOLES", "14:00", "18:00", "PRESENCIAL");
            crearDisponibilidad(pr, "VIERNES", "13:00", "14:00", "ALMUERZO");
        }
        for (User pr : profesores.subList(7, 14)) {
            crearDisponibilidad(pr, "MARTES", "09:00", "13:00", "PRESENCIAL");
            crearDisponibilidad(pr, "JUEVES", "14:00", "18:00", "VIRTUAL");
            crearDisponibilidad(pr, "LUNES", "13:00", "14:00", "ALMUERZO");
        }
        for (User pr : profesores.subList(14, 20)) {
            crearDisponibilidad(pr, "MIERCOLES", "08:00", "12:00", "VIRTUAL");
            crearDisponibilidad(pr, "VIERNES", "09:00", "13:00", "PRESENCIAL");
            crearDisponibilidad(pr, "JUEVES", "13:00", "14:00", "ALMUERZO");
        }
    }

    /** Horarios de tarde predominantes, alternando dos franjas segun la posicion en la lista. */
    private void sembrarDisponibilidadAlumnos(List<User> alumnos) {
        for (int i = 0; i < alumnos.size(); i++) {
            User a = alumnos.get(i);
            if (i % 2 == 0) {
                crearDisponibilidad(a, "LUNES", "16:00", "18:00", "VIRTUAL");
                crearDisponibilidad(a, "MIERCOLES", "16:00", "18:00", "VIRTUAL");
            } else {
                crearDisponibilidad(a, "MARTES", "17:00", "19:00", "PRESENCIAL");
                crearDisponibilidad(a, "JUEVES", "17:00", "19:00", "PRESENCIAL");
            }
        }
    }

    // ====================================================================================
    // Cursos oficiales, inscripciones, propuestas y materiales
    // ====================================================================================

    private void sembrarCursosEInscripciones(List<User> profesores, List<User> alumnos) {
        Schedule h1 = crearHorario(profesores.get(0), DayOfWeekEnum.LUNES, "09:00", "10:30",
                "Ingles Basico A1", Language.ENGLISH, "Nivel inicial.", 15);
        Schedule h2 = crearHorario(profesores.get(1), DayOfWeekEnum.MIERCOLES, "15:00", "16:30",
                "Frances B1", Language.FRENCH, "Intermedio.", 15);
        Schedule h3 = crearHorario(profesores.get(4), DayOfWeekEnum.VIERNES, "10:00", "11:30",
                "Espanol Avanzado", Language.SPANISH, "Avanzado.", 20);
        Schedule h4 = crearHorario(profesores.get(9), DayOfWeekEnum.JUEVES, "09:00", "12:00",
                "Ingles Intensivo", Language.ENGLISH, "Bootcamp.", 20);

        // Repartir a los 35 alumnos en las 4 clases: 10 + 10 + 10 + 5
        inscribirRango(alumnos, 0, 10, h1);
        inscribirRango(alumnos, 10, 20, h2);
        inscribirRango(alumnos, 20, 30, h3);
        inscribirRango(alumnos, 30, 35, h4);
    }

    private void inscribirRango(List<User> alumnos, int desde, int hasta, Schedule schedule) {
        for (User alumno : alumnos.subList(desde, hasta)) {
            scheduleService.enroll(alumno, schedule);
        }
    }

    private void sembrarPropuestasYMateriales(List<User> profesores) {
        crearPropuesta(profesores.get(0), ProposalType.ACADEMIC, "Taller intensivo",
                "Propongo un taller los jueves.", ProposalStatus.PENDING, null);
        crearPropuesta(profesores.get(5), ProposalType.CURRICULAR, "Nuevo libro",
                "Actualizar material de B2.", ProposalStatus.APPROVED, "Aprobado para 2027.");

        crearMaterial(profesores.get(1), "Guia Vocabulario", "PDF interactivo.",
                MaterialCategory.GUIDE, Language.FRENCH, "https://link.com");
        crearMaterial(profesores.get(9), "Podcast Listening", "Audio para practicar.",
                MaterialCategory.VIDEO, Language.ENGLISH, "https://link.com/audio");
    }

    // ====================================================================================
    // Control de tiempo: turnos ya fichados y reportes semanales de ejemplo, para que el
    // modulo se vea con datos reales desde el primer arranque (no vacio).
    // ====================================================================================

    private void sembrarControlDeTiempo(List<User> practicantes, List<User> profesores) {
        LocalDate lunesEstaSemana = ReporteSemanalService.lunesDeLaSemana(LocalDate.now());

        // Practicante: semana en curso, con turnos ya fichados hasta hoy y un
        // borrador de reporte a medio escribir (nunca enviado todavia).
        User alvaro = practicantes.get(0);
        sembrarSemanaDeTurnos(alvaro, lunesEstaSemana, LocalDate.now());
        ReporteSemanal borrador = new ReporteSemanal();
        borrador.setColaborador(alvaro);
        borrador.setSemanaInicio(lunesEstaSemana);
        borrador.setNotasLunes("Reunion de equipo y avance de fichas de vocabulario.");
        borrador.setNotasMartes("Apoyo presencial al grupo de la tarde.");
        reporteSemanalRepository.save(borrador);

        // Profesor: semana pasada ya cerrada, reporte ENVIADO con una solicitud de
        // ajuste (se le olvido marcar un dia), esperando revision del admin.
        User maria = profesores.get(0);
        LocalDate semanaPasada = lunesEstaSemana.minusWeeks(1);
        sembrarSemanaDeTurnos(maria, semanaPasada, semanaPasada.plusDays(4));
        ReporteSemanal pendiente = new ReporteSemanal();
        pendiente.setColaborador(maria);
        pendiente.setSemanaInicio(semanaPasada);
        pendiente.setNotasLunes("Clases de frances basico, grupo A1.");
        pendiente.setNotasMartes("Correccion de examenes y planificacion de la unidad 3.");
        pendiente.setNotasMiercoles("Clase presencial B1 y tutoria individual.");
        pendiente.setMinutosAjusteSolicitado(90);
        pendiente.setComentarioAjuste("El jueves me quede apoyando a un alumno despues de mi horario y se me olvido marcar la salida.");
        pendiente.setEstado(EstadoReporte.PENDIENTE_REVISION);
        pendiente.setEnviadoEn(semanaPasada.plusDays(5).atTime(10, 30));
        reporteSemanalRepository.save(pendiente);

        // Otro profesor: hace dos semanas, ciclo completo ya APROBADO por el admin.
        User roberto = profesores.get(2);
        LocalDate hace2Semanas = lunesEstaSemana.minusWeeks(2);
        sembrarSemanaDeTurnos(roberto, hace2Semanas, hace2Semanas.plusDays(4));
        ReporteSemanal aprobado = new ReporteSemanal();
        aprobado.setColaborador(roberto);
        aprobado.setSemanaInicio(hace2Semanas);
        aprobado.setNotasLunes("Clases de espanol para principiantes.");
        aprobado.setNotasMartes("Reunion de coordinacion academica.");
        aprobado.setNotasMiercoles("Clases y revision de material.");
        aprobado.setNotasJueves("Clases presenciales.");
        aprobado.setNotasViernes("Cierre de semana y entrega de notas.");
        aprobado.setEstado(EstadoReporte.APROBADO);
        aprobado.setEnviadoEn(hace2Semanas.plusDays(5).atTime(9, 0));
        aprobado.setMinutosFinalesAprobados(20 * 60);
        aprobado.setComentarioAdmin("Buen trabajo, horas aprobadas tal como se registraron.");
        aprobado.setRevisadoEn(hace2Semanas.plusDays(6).atTime(11, 0));
        reporteSemanalRepository.save(aprobado);
    }

    /**
     * Turnos LUNES..VIERNES para un colaborador entre esas fechas (sin pasarse de
     * hastaFecha). Si hastaFecha es hoy, el turno de hoy se deja EN_TURNO (sin
     * hora de fin) para que el widget del reloj se vea corriendo de verdad al
     * abrir la app; el resto quedan FINALIZADO con un dia normal de 9:05 a 17:30.
     */
    private void sembrarSemanaDeTurnos(User colaborador, LocalDate lunes, LocalDate hastaFecha) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();

        for (int i = 0; i <= 4; i++) {
            LocalDate fecha = lunes.plusDays(i);
            if (fecha.isAfter(hastaFecha)) {
                break;
            }

            RegistroTiempo t = new RegistroTiempo();
            t.setColaborador(colaborador);
            t.setFecha(fecha);

            if (fecha.equals(hoy)) {
                LocalDateTime inicioHoy = ahora.toLocalTime().isBefore(LocalTime.of(2, 0))
                        ? fecha.atTime(0, 30) : ahora.minusHours(2);
                t.setHoraInicio(inicioHoy);
                if (ahora.toLocalTime().isAfter(LocalTime.of(13, 0))) {
                    t.setInicioAlmuerzo(fecha.atTime(13, 0));
                    t.setFinAlmuerzo(fecha.atTime(13, 45));
                }
                t.setEstado(EstadoTurno.EN_TURNO);
            } else {
                t.setHoraInicio(fecha.atTime(9, 5));
                t.setInicioAlmuerzo(fecha.atTime(13, 0));
                t.setFinAlmuerzo(fecha.atTime(14, 2));
                t.setHoraFin(fecha.atTime(17, 30));
                t.setEstado(EstadoTurno.FINALIZADO);
            }
            registroTiempoRepository.save(t);
        }
    }

    // ====================================================================================
    // Metodos auxiliares de bajo nivel
    // ====================================================================================

    private void crearDisponibilidad(User user, String dia, String horaInicioStr, String horaFinStr, String modalidad) {
        LocalTime inicio = LocalTime.parse(horaInicioStr);
        LocalTime fin = LocalTime.parse(horaFinStr);

        while (inicio.isBefore(fin)) {
            String horaFormateada = inicio.toString();
            String horaFinFormateada = inicio.plusHours(1).toString();
            horarioService.guardarOActualizarCelda(user, dia, horaFormateada, horaFinFormateada, modalidad, null);
            inicio = inicio.plusHours(1);
        }
    }

    private Schedule crearHorario(User teacher, DayOfWeekEnum day, String start, String end,
                                  String subject, Language lang, String desc, int max) {
        Schedule s = new Schedule();
        s.setTeacher(teacher);
        s.setDayOfWeek(day);
        s.setStartTime(LocalTime.parse(start));
        s.setEndTime(LocalTime.parse(end));
        s.setSubject(subject);
        s.setLanguage(lang);
        s.setDescription(desc);
        s.setMaxStudents(max);
        return scheduleService.save(s);
    }

    private void crearPropuesta(User teacher, ProposalType type, String title, String desc,
                                ProposalStatus status, String comment) {
        Proposal p = new Proposal();
        p.setTeacher(teacher);
        p.setType(type);
        p.setTitle(title);
        p.setDescription(desc);
        p.setStatus(status);
        p.setAdminComment(comment);
        proposalService.save(p);
    }

    private void crearMaterial(User teacher, String title, String desc, MaterialCategory cat,
                               Language lang, String link) {
        Material m = new Material();
        m.setTeacher(teacher);
        m.setTitle(title);
        m.setDescription(desc);
        m.setCategory(cat);
        m.setLanguage(lang);
        m.setExternalLink(link);
        materialService.save(m);
    }
}

