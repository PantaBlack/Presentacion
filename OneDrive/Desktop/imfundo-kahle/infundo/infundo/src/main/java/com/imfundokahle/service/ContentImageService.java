package com.imfundokahle.service;

import com.imfundokahle.model.ContentImage;
import com.imfundokahle.repository.ContentImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Imagenes editables de la pagina publica de inicio. Guardadas en la base de
 * datos (mismo enfoque que los archivos de Material) para que subir una
 * imagen nueva funcione igual en desarrollo y en un jar empaquetado.
 */
@Service
public class ContentImageService {

    public static class ImagenDef {
        private final String clave;
        private final String etiqueta;
        private final boolean opcional;

        public ImagenDef(String clave, String etiqueta) {
            this(clave, etiqueta, false);
        }

        /** "opcional" = no trae imagen de fabrica y se puede quitar despues de subida (vuelve al fondo/estilo por defecto). */
        public ImagenDef(String clave, String etiqueta, boolean opcional) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.opcional = opcional;
        }

        public String getClave() { return clave; }
        public String getEtiqueta() { return etiqueta; }
        public boolean isOpcional() { return opcional; }
    }

    /** Las imagenes que hoy tiene el home; agregar una nueva aqui basta para que aparezca en el panel. */
    public static final List<ImagenDef> IMAGENES = List.of(
            new ImagenDef("hero", "Imagen principal (hero)"),
            new ImagenDef("fondo-hero", "Fondo de la portada (opcional, foto detras del texto principal)", true),
            new ImagenDef("fondo-promo", "Fondo del banner promocional (opcional, se ve junto al color de temporada)", true),
            new ImagenDef("fondo-stats", "Fondo de 'Nuestro impacto' (opcional)", true),
            new ImagenDef("flag-es", "Tarjeta de curso - Espanol"),
            new ImagenDef("flag-fr", "Tarjeta de curso - Frances"),
            new ImagenDef("flag-en", "Tarjeta de curso - Ingles"),
            new ImagenDef("libro-1", "Libro - Viva Latinoamerica 1"),
            new ImagenDef("libro-2", "Libro - Viva Latinoamerica 2"),
            new ImagenDef("libro-3", "Libro - Viva Latinoamerica 3"),
            new ImagenDef("libro-palabras", "Libro - Palabras y expresiones"),
            new ImagenDef("libro-survival", "Libro - Survival Spanish"),
            new ImagenDef("libros-grupo", "Foto de grupo de la coleccion"),
            new ImagenDef("banner-profesor", "Fondo del panel de Profesor (opcional, foto detras del encabezado)", true),
            new ImagenDef("banner-alumno", "Fondo del panel de Alumno (opcional, foto detras del encabezado)", true),
            new ImagenDef("banner-practicante", "Fondo del panel de Practicante (opcional, foto detras del encabezado)", true),
            new ImagenDef("banner-admin", "Fondo del panel de Administrador (opcional, foto detras del encabezado)", true),
            new ImagenDef("welcome-student-img", "Foto de la tarjeta de bienvenida del panel de Alumno (opcional)", true),
            new ImagenDef("welcome-teacher-img", "Foto de la tarjeta de bienvenida del panel de Profesor (opcional)", true),
            new ImagenDef("welcome-practicante-img", "Foto de la tarjeta de bienvenida del panel de Practicante (opcional)", true)
    );

    private final ContentImageRepository repository;
    private final MaterialFileValidator validator;

    public ContentImageService(ContentImageRepository repository, MaterialFileValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public boolean esClaveValida(String key) {
        return IMAGENES.stream().anyMatch(i -> i.getClave().equals(key));
    }

    public Optional<ContentImage> findByKey(String key) {
        return repository.findByImageKey(key);
    }

    public Map<String, ContentImage> getTodasComoMapa() {
        Map<String, ContentImage> mapa = new LinkedHashMap<>();
        for (ContentImage img : repository.findAll()) {
            mapa.put(img.getImageKey(), img);
        }
        return mapa;
    }

    /** Si no existe una imagen con esa clave todavia, la crea con los bytes por defecto (arranque de la app). */
    @Transactional
    public void sembrarSiNoExiste(String key, byte[] bytesPorDefecto, String contentType) {
        if (repository.existsByImageKey(key)) {
            return;
        }
        ContentImage img = new ContentImage();
        img.setImageKey(key);
        img.setData(bytesPorDefecto);
        img.setContentType(contentType);
        repository.save(img);
    }

    /**
     * Reemplaza la imagen. Devuelve false (sin guardar nada) si la clave no es
     * una de las reconocidas, o si el contenido no es una imagen real segun sus
     * primeros bytes (nunca se confia en la extension del nombre ni en el
     * Content-Type que declara el navegador).
     */
    @Transactional
    public boolean reemplazar(String key, byte[] contenido) {
        if (!esClaveValida(key) || !validator.validarTamano(contenido).isValido()) {
            return false;
        }
        MaterialFileValidator.Resultado tipo = validator.validarTipo(contenido);
        if (!tipo.isValido() || !tipo.getTipoDetectado().startsWith("image/")) {
            return false;
        }
        ContentImage img = repository.findByImageKey(key).orElseGet(() -> {
            ContentImage nuevo = new ContentImage();
            nuevo.setImageKey(key);
            return nuevo;
        });
        img.setData(contenido);
        img.setContentType(tipo.getTipoDetectado());
        img.setVersion(System.currentTimeMillis());
        repository.save(img);
        return true;
    }

    /**
     * Quita una imagen subida (solo tiene sentido para las marcadas como
     * "opcional": las demas son piezas fijas del diseno y siempre deben tener
     * algo que mostrar). Al quitarla, el sitio vuelve a su estilo por defecto
     * para esa clave (ej. el fondo de portada vuelve al degradado original).
     */
    @Transactional
    public boolean eliminar(String key) {
        ImagenDef def = IMAGENES.stream().filter(i -> i.getClave().equals(key)).findFirst().orElse(null);
        if (def == null || !def.isOpcional()) {
            return false;
        }
        repository.findByImageKey(key).ifPresent(repository::delete);
        return true;
    }

    /** Version actual de cada imagen (para el "?v=" que evita que el navegador muestre una version vieja en cache). */
    public Map<String, Long> getVersiones() {
        Map<String, Long> versiones = new LinkedHashMap<>();
        for (ContentImage img : repository.findAll()) {
            versiones.put(img.getImageKey(), img.getVersion());
        }
        return versiones;
    }
}
