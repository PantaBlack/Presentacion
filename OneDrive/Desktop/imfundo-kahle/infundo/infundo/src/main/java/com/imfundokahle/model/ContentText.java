package com.imfundokahle.model;

import jakarta.persistence.*;

/**
 * Sobreescritura editable (desde el panel de admin) de un texto de la pagina
 * publica de inicio, identificado por la misma clave que usa el archivo de
 * mensajes (ej. "landing.hero.title"). Un valor nulo o vacio en un idioma
 * significa "usar el texto de fabrica de ese idioma" (el de messages*.properties);
 * asi el admin puede cambiar solo el idioma que le interesa sin tener que
 * rellenar los tres.
 */
@Entity
@Table(name = "content_text")
public class ContentText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String messageKey;

    @Column(length = 2000)
    private String valorEs;

    @Column(length = 2000)
    private String valorFr;

    @Column(length = 2000)
    private String valorEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String messageKey) { this.messageKey = messageKey; }

    public String getValorEs() { return valorEs; }
    public void setValorEs(String valorEs) { this.valorEs = valorEs; }

    public String getValorFr() { return valorFr; }
    public void setValorFr(String valorFr) { this.valorFr = valorFr; }

    public String getValorEn() { return valorEn; }
    public void setValorEn(String valorEn) { this.valorEn = valorEn; }

    /** Valor para un idioma dado ("es"/"fr"/"en"), o null si no aplica ese codigo. */
    public String getValorParaIdioma(String idioma) {
        if (idioma == null) {
            return null;
        }
        return switch (idioma) {
            case "es" -> valorEs;
            case "fr" -> valorFr;
            case "en" -> valorEn;
            default -> null;
        };
    }
}
