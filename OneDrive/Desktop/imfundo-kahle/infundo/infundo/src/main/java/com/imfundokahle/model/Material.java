package com.imfundokahle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad Material: recurso educativo compartido por un profesor.
 * Puede tener un enlace externo, un archivo adjunto (guardado en la base
 * de datos), o ambos.
 */
@Entity
@Table(name = "materials")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /**
     * Clase a la que pertenece este material (opcional). Si es null, el material es
     * general (se rige solo por "visibility", como antes). Si tiene una clase, solo
     * lo ven el profesor dueño, el admin, y los alumnos activamente inscritos en esa
     * clase especifica — util para apuntes/tareas propios de un curso puntual.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialCategory category;

    /** Enlace externo al material (opcional). */
    private String externalLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** Quien puede ver este material; por defecto, todo el mundo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialVisibility visibility = MaterialVisibility.EVERYONE;

    // ---- Archivo adjunto (opcional) ----
    private String fileName;
    private String fileType;
    private Long fileSize;

    @Lob
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Material() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** True si este material tiene un archivo propio (no solo un enlace externo). */
    @Transient
    public boolean isHasFile() {
        return fileData != null && fileData.length > 0;
    }

    /** Tamano legible del archivo adjunto (ej. "2.3 MB"), o cadena vacia si no hay archivo. */
    @Transient
    public String getFileSizeLabel() {
        if (fileSize == null || fileSize <= 0) return "";
        double kb = fileSize / 1024.0;
        if (kb < 1024) return String.format("%.0f KB", kb);
        return String.format("%.1f MB", kb / 1024.0);
    }

    /** Devuelve el emoji-icono segun la categoria. */
    @Transient
    public String getIcon() {
        if (category == null) return "📄";
        switch (category) {
            case BOOK: return "📚";
            case GUIDE: return "📋";
            case EXERCISE: return "✏️";
            case VIDEO: return "🎬";
            default: return "📄";
        }
    }

    // ---- Getters y Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MaterialCategory getCategory() { return category; }
    public void setCategory(MaterialCategory category) { this.category = category; }

    public String getExternalLink() { return externalLink; }
    public void setExternalLink(String externalLink) { this.externalLink = externalLink; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public MaterialVisibility getVisibility() { return visibility; }
    public void setVisibility(MaterialVisibility visibility) { this.visibility = visibility; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
