package com.imfundokahle.model;

import jakarta.persistence.*;

/**
 * Elemento libre que el admin agrega a la "zona de diseno libre" de la pagina
 * de inicio, sin tocar codigo: un bloque de texto o una imagen que puede
 * arrastrar y rotar a su gusto dentro del lienzo (ver la seccion final de
 * index.html, justo antes del pie de pagina). A diferencia de los textos de
 * {@link ContentText}, esto no edita un campo ya existente del diseno: agrega
 * contenido nuevo.
 */
@Entity
@Table(name = "custom_section")
public class CustomSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "texto" o "imagen". */
    @Column(nullable = false, length = 20)
    private String tipo = "texto";

    @Column(length = 200)
    private String titulo;

    /** Texto libre (solo tipo "texto"); se muestra respetando los saltos de linea. */
    @Column(length = 4000)
    private String texto;

    /** Bytes de la imagen (solo tipo "imagen"); mismo enfoque que ContentImage. */
    @Lob
    private byte[] imagenData;

    private String imagenContentType;

    /** Cambia con cada reemplazo de imagen; se usa como "?v=" contra cache vieja del navegador. */
    private long imagenVersion = System.currentTimeMillis();

    /** Posicion dentro del lienzo, en porcentaje (0-100) desde la esquina superior izquierda. */
    @Column(nullable = false)
    private double posX = 10;

    @Column(nullable = false)
    private double posY = 10;

    /** Ancho del elemento dentro del lienzo, en porcentaje del lienzo. */
    @Column(nullable = false)
    private double ancho = 30;

    /** Rotacion en grados (puede ser negativa). */
    @Column(nullable = false)
    private double rotacion = 0;

    /** Orden de creacion (desempate de capas: el mas nuevo queda encima). */
    @Column(nullable = false)
    private int orden;

    /** Permite ocultarla temporalmente sin borrarla. */
    @Column(nullable = false)
    private boolean visible = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public byte[] getImagenData() { return imagenData; }
    public void setImagenData(byte[] imagenData) { this.imagenData = imagenData; }

    public String getImagenContentType() { return imagenContentType; }
    public void setImagenContentType(String imagenContentType) { this.imagenContentType = imagenContentType; }

    public long getImagenVersion() { return imagenVersion; }
    public void setImagenVersion(long imagenVersion) { this.imagenVersion = imagenVersion; }

    public double getPosX() { return posX; }
    public void setPosX(double posX) { this.posX = posX; }

    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }

    public double getAncho() { return ancho; }
    public void setAncho(double ancho) { this.ancho = ancho; }

    public double getRotacion() { return rotacion; }
    public void setRotacion(double rotacion) { this.rotacion = rotacion; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isImagen() { return "imagen".equals(tipo); }
}
