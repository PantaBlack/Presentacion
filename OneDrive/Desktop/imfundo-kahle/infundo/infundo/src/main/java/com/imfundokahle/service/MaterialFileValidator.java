package com.imfundokahle.service;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Valida el archivo adjunto de un material educativo mirando su contenido real
 * (magic bytes), no la extension del nombre ni el Content-Type que declara el
 * navegador: ambos los controla quien sube el archivo y se falsean con un clic.
 * <p>
 * Solo se aceptan los formatos que la aplicacion realmente sabe mostrar/descargar
 * (documentos, imagenes, audio, video). Cualquier otra cosa -incluido un ejecutable
 * o script renombrado con extension ".pdf"- se rechaza aqui, antes de guardarse.
 */
@Component
public class MaterialFileValidator {

    /** Debe coincidir con spring.servlet.multipart.max-file-size (defensa en profundidad). */
    public static final long MAX_BYTES = 15L * 1024 * 1024;

    public static final class Resultado {
        private final boolean valido;
        private final String tipoDetectado;

        private Resultado(boolean valido, String tipoDetectado) {
            this.valido = valido;
            this.tipoDetectado = tipoDetectado;
        }

        public boolean isValido() { return valido; }

        /** Content-Type real, determinado por el servidor a partir del contenido (no el que envio el cliente). */
        public String getTipoDetectado() { return tipoDetectado; }
    }

    /** Archivo vacio o demasiado grande: ni se intenta identificar el tipo. */
    public Resultado validarTamano(byte[] contenido) {
        if (contenido == null || contenido.length == 0 || contenido.length > MAX_BYTES) {
            return new Resultado(false, null);
        }
        return new Resultado(true, null);
    }

    /**
     * Identifica el tipo real del archivo por sus primeros bytes. Devuelve un
     * resultado invalido (tipoDetectado null) si el contenido no coincide con
     * ninguno de los formatos permitidos.
     */
    public Resultado validarTipo(byte[] contenido) {
        String tipo = detectarTipo(contenido);
        return new Resultado(tipo != null, tipo);
    }

    private String detectarTipo(byte[] b) {
        if (b == null || b.length < 4) {
            return null;
        }

        // ---- Documentos ----
        if (empieza(b, 0x25, 0x50, 0x44, 0x46)) {                 // %PDF
            return "application/pdf";
        }
        if (empieza(b, 0xD0, 0xCF, 0x11, 0xE0)) {                 // OLE2: .doc/.xls/.ppt antiguos
            return "application/msword";
        }
        if (empieza(b, 0x50, 0x4B, 0x03, 0x04)) {                 // ZIP: puede ser docx/pptx/xlsx
            String ooxml = detectarOoxml(b);
            if (ooxml != null) {
                return ooxml;
            }
            return null; // zip generico: no es un formato que la app deba aceptar
        }

        // ---- Imagenes ----
        if (empieza(b, 0x89, 0x50, 0x4E, 0x47)) {                 // PNG
            return "image/png";
        }
        if (empieza(b, 0xFF, 0xD8, 0xFF)) {                       // JPEG
            return "image/jpeg";
        }
        if (empiezaAscii(b, "GIF87a") || empiezaAscii(b, "GIF89a")) {
            return "image/gif";
        }
        if (empieza(b, 0x52, 0x49, 0x46, 0x46) && contieneAscii(b, 8, "WEBP")) {
            return "image/webp";
        }

        // ---- Audio ----
        if (empieza(b, 0x52, 0x49, 0x46, 0x46) && contieneAscii(b, 8, "WAVE")) {
            return "audio/wav";
        }
        if (empieza(b, 0x49, 0x44, 0x33)                          // ID3 (mp3 con etiquetas)
                || empieza(b, 0xFF, 0xFB) || empieza(b, 0xFF, 0xF3) || empieza(b, 0xFF, 0xF2)) { // frame MPEG crudo
            return "audio/mpeg";
        }
        if (empieza(b, 0x4F, 0x67, 0x67, 0x53)) {                 // OggS
            return "audio/ogg";
        }

        // ---- Video ----
        if (esMp4(b)) {
            return "video/mp4";
        }
        if (empieza(b, 0x1A, 0x45, 0xDF, 0xA3)) {                 // EBML: webm/mkv
            return "video/webm";
        }

        return null;
    }

    /**
     * Un .docx/.pptx/.xlsx real es un ZIP con carpetas internas propias de Office
     * (word/, ppt/, xl/); un ZIP cualquiera renombrado con esa extension no las tiene.
     */
    private String detectarOoxml(byte[] contenido) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("word/")) {
                    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                }
                if (name.startsWith("ppt/")) {
                    return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                }
                if (name.startsWith("xl/")) {
                    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private boolean esMp4(byte[] b) {
        return b.length > 11 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p';
    }

    private boolean empieza(byte[] b, int... firma) {
        if (b.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((b[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean empiezaAscii(byte[] b, String prefijo) {
        return contieneAscii(b, 0, prefijo);
    }

    private boolean contieneAscii(byte[] b, int offset, String texto) {
        byte[] esperado = texto.getBytes(StandardCharsets.US_ASCII);
        if (b.length < offset + esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if (b[offset + i] != esperado[i]) {
                return false;
            }
        }
        return true;
    }
}
