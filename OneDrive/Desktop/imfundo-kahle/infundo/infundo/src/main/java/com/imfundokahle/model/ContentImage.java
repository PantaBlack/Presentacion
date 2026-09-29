package com.imfundokahle.model;

import jakarta.persistence.*;

/**
 * Imagen editable (desde el panel de admin) de la pagina publica de inicio,
 * identificada por una clave fija (ej. "hero", "flag-es"). Igual que los
 * archivos de material, el contenido se guarda en la base de datos, no en
 * disco: asi funciona igual en desarrollo y en un jar empaquetado en
 * produccion, donde los recursos estaticos no son escribibles en tiempo
 * de ejecucion.
 */
@Entity
@Table(name = "content_image")
public class ContentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String imageKey;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private String contentType;

    /**
     * Se actualiza cada vez que se reemplaza la imagen. Se usa como "?v=" en la
     * URL publica para que el navegador SIEMPRE muestre la version correcta:
     * sin esto, el navegador podia quedarse mostrando la imagen vieja desde su
     * cache aunque el admin ya hubiera subido una nueva (la URL nunca cambiaba).
     */
    @Column(nullable = false)
    private long version = System.currentTimeMillis();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
