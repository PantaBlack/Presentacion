package com.imfundokahle.model;

import jakarta.persistence.*;

/**
 * Configuracion del banner promocional del home: siempre hay una sola fila
 * (la primera que exista), con el tema visual que eligio el admin.
 */
@Entity
@Table(name = "promo_banner_config")
public class PromoBannerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromoTema tema = PromoTema.NORMAL;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PromoTema getTema() { return tema; }
    public void setTema(PromoTema tema) { this.tema = tema; }
}
