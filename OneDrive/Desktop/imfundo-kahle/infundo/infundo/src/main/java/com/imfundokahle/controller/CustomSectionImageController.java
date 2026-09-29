package com.imfundokahle.controller;

import com.imfundokahle.model.CustomSection;
import com.imfundokahle.service.CustomSectionService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;

/**
 * Sirve las imagenes de los elementos de la "zona de diseno libre" del home
 * (ver CustomSection). Es publico (sin login) porque el home es publico;
 * mismo enfoque de cache que ContentImageController.
 */
@Controller
public class CustomSectionImageController {

    private final CustomSectionService customSectionService;

    public CustomSectionImageController(CustomSectionService customSectionService) {
        this.customSectionService = customSectionService;
    }

    @GetMapping("/contenido/seccion-imagen/{id}")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id) {
        CustomSection s = customSectionService.buscar(id);
        if (s == null || !s.isImagen() || s.getImagenData() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType tipo;
        try {
            tipo = MediaType.parseMediaType(s.getImagenContentType());
        } catch (Exception e) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(s.getImagenData());
    }
}
