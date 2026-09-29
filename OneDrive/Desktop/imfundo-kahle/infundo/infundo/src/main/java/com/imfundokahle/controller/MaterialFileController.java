package com.imfundokahle.controller;

import com.imfundokahle.model.Material;
import com.imfundokahle.model.User;
import com.imfundokahle.service.MaterialService;
import com.imfundokahle.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.net.URLEncoder;

/**
 * Descarga de archivos adjuntos a un material. Accesible a cualquier usuario
 * autenticado (profesor, alumno, practicante o admin); la visibilidad real
 * la decide {@link MaterialService#esVisiblePara}, no el rol en si.
 */
@Controller
@RequestMapping("/materiales")
public class MaterialFileController {

    private final MaterialService materialService;
    private final UserService userService;

    public MaterialFileController(MaterialService materialService, UserService userService) {
        this.materialService = materialService;
        this.userService = userService;
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id, Principal principal) {
        Material material = materialService.findById(id);
        if (material == null || !material.isHasFile()) {
            return ResponseEntity.notFound().build();
        }

        User usuario = userService.findByEmail(principal.getName()).orElse(null);
        if (usuario == null || !materialService.esVisiblePara(material, usuario)) {
            return ResponseEntity.status(403).build();
        }

        String nombreArchivo = material.getFileName() != null ? material.getFileName() : "material";
        // Nombre de respaldo solo-ASCII (para clientes viejos que no leen filename*) mas la
        // version UTF-8 completa (para que los acentos y espacios se vean bien en el resto).
        String nombreAscii = nombreArchivo.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
        String nombreCodificado = URLEncoder.encode(nombreArchivo, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType tipo;
        try {
            tipo = material.getFileType() != null ? MediaType.parseMediaType(material.getFileType()) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreAscii + "\"; filename*=UTF-8''" + nombreCodificado)
                // "attachment" ya evita que el navegador lo renderice inline; nosniff es
                // una segunda capa para que tampoco intente adivinar el tipo por su cuenta.
                .header("X-Content-Type-Options", "nosniff")
                .body(material.getFileData());
    }
}
