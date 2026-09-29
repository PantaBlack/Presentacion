package com.imfundokahle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad Horario: una franja horaria de clase creada por un profesor.
 */
@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Profesor propietario del horario. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeekEnum dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** Materia o tema de la clase. */
    @Column(nullable = false)
    private String subject;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** Numero maximo de alumnos permitidos. */
    private int maxStudents = 10;

    /** Enlace de la videollamada (Google Meet, Zoom, etc.), opcional. Solo lo ve el
     *  profesor dueño y los alumnos activamente inscritos en esta clase. */
    @Column(name = "meet_link", length = 500)
    private String meetLink;

    /**
     * Clase personalizada (1 a 1 o 1 a 2), tipicamente dada por un practicante: no
     * aparece en el listado abierto de "clases disponibles" y el alumno no se
     * autoinscribe, sino que quien crea la clase lo asigna directamente.
     */
    @Column(nullable = false)
    private boolean personalizada = false;

    /** Ultima fecha en que se mando el correo recordatorio de esta clase (evita mandarlo
     *  dos veces la misma semana; ver ClaseReminderScheduler). */
    @Column(name = "ultimo_recordatorio")
    private java.time.LocalDate ultimoRecordatorio;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Schedule() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ---- Getters y Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public DayOfWeekEnum getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeekEnum dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public int getMaxStudents() { return maxStudents; }
    public void setMaxStudents(int maxStudents) { this.maxStudents = maxStudents; }

    public String getMeetLink() { return meetLink; }
    public void setMeetLink(String meetLink) { this.meetLink = meetLink; }

    public boolean isPersonalizada() { return personalizada; }
    public void setPersonalizada(boolean personalizada) { this.personalizada = personalizada; }

    public java.time.LocalDate getUltimoRecordatorio() { return ultimoRecordatorio; }
    public void setUltimoRecordatorio(java.time.LocalDate ultimoRecordatorio) { this.ultimoRecordatorio = ultimoRecordatorio; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
