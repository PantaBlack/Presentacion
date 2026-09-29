package com.imfundokahle.config;

import com.imfundokahle.service.ContentImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, si alguna de las imagenes editables del home todavia no existe
 * en la base de datos (primer arranque, o una base de datos nueva), la crea
 * con la imagen original que ya trae el proyecto. Asi el endpoint dinamico
 * que sirve estas imagenes siempre tiene algo que mostrar, incluso antes de
 * que un admin suba un reemplazo desde el panel.
 */
@Component
public class ContentImageSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentImageSeeder.class);

    private final ContentImageService contentImageService;

    public ContentImageSeeder(ContentImageService contentImageService) {
        this.contentImageService = contentImageService;
    }

    @Override
    public void run(String... args) {
        sembrar("hero", "img/iconos/home.jpeg", "image/jpeg");
        sembrar("flag-es", "img/iconos/español.jpeg", "image/jpeg");
        sembrar("flag-fr", "img/iconos/frances.jpeg", "image/jpeg");
        sembrar("flag-en", "img/iconos/ingles.jpeg", "image/jpeg");
        sembrar("libro-1", "img/libros/libro-1.jpg", "image/jpeg");
        sembrar("libro-2", "img/libros/libro-2.jpg", "image/jpeg");
        sembrar("libro-3", "img/libros/libro-3.jpg", "image/jpeg");
        sembrar("libro-palabras", "img/libros/libro-palabras.jpg", "image/jpeg");
        sembrar("libro-survival", "img/libros/libro-survival.jpg", "image/jpeg");
        sembrar("libros-grupo", "img/libros/libros-grupo.png", "image/png");
    }

    private void sembrar(String key, String rutaClasspathEstatico, String contentType) {
        try {
            ClassPathResource recurso = new ClassPathResource("static/" + rutaClasspathEstatico);
            byte[] bytes = recurso.getInputStream().readAllBytes();
            contentImageService.sembrarSiNoExiste(key, bytes, contentType);
        } catch (Exception e) {
            log.warn("No se pudo cargar la imagen inicial '{}' para la clave '{}': {}", rutaClasspathEstatico, key, e.getMessage());
        }
    }
}
