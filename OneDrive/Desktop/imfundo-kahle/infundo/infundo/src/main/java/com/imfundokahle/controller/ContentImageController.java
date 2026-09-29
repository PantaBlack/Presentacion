package com.imfundokahle.controller;

import com.imfundokahle.model.ContentImage;
import com.imfundokahle.service.ContentImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;

/**
 * Sirve las imagenes editables del home publico (ver ContentImageService).
 * Es publico (sin login) porque el propio home lo es.
 */
@Controller
@RequestMapping("/contenido")
public class ContentImageController {

    private final ContentImageService contentImageService;

    public ContentImageController(ContentImageService contentImageService) {
        this.contentImageService = contentImageService;
    }

    @GetMapping("/imagen/{key}")
    public ResponseEntity<byte[]> imagen(@PathVariable String key) {
        return contentImageService.findByKey(key)
                .map(ContentImageController::responder)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ResponseEntity<byte[]> responder(ContentImage img) {
        MediaType tipo;
        try {
            tipo = MediaType.parseMediaType(img.getContentType());
        } catch (Exception e) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                // Cache larga y segura: la URL incluye "?v=" con la version de la imagen
                // (ver ContentImageService.getVersiones), asi que cuando el admin sube una
                // nueva, la URL cambia y el navegador la vuelve a pedir sola. Sin este
                // truco, "cachear fuerte" dejaba pegada la imagen vieja en el navegador
                // del usuario aunque el admin ya hubiera subido el reemplazo.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                // El tipo real ya se valido por sus primeros bytes al subirla
                // (MaterialFileValidator), pero igual le decimos al navegador que no
                // intente adivinar otro tipo por su cuenta.
                .header("X-Content-Type-Options", "nosniff")
                .body(img.getData());
    }
}
