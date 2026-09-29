package com.imfundokahle.service;

import com.imfundokahle.model.CustomSection;
import com.imfundokahle.repository.CustomSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Elementos libres (texto o imagen) que el admin agrega y puede arrastrar y
 * rotar en la "zona de diseno libre" del home (ver {@link CustomSection}).
 * A diferencia de ContentTextService, esto no edita campos existentes del
 * diseno: crea elementos nuevos, posicionados donde el admin elija.
 */
@Service
public class CustomSectionService {

    private static final int MAX_TITULO = 200;
    private static final int MAX_TEXTO = 4000;

    /** Limites del lienzo, para que un elemento nunca quede totalmente fuera de vista. */
    private static final double POS_MIN = 0;
    private static final double POS_MAX = 92;
    private static final double ANCHO_MIN = 10;
    private static final double ANCHO_MAX = 90;
    private static final double ROTACION_MAX = 45;

    private final CustomSectionRepository repository;
    private final MaterialFileValidator validator;

    public CustomSectionService(CustomSectionRepository repository, MaterialFileValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<CustomSection> listarTodas() {
        return repository.findAllByOrderByOrdenAsc();
    }

    /** Solo las visibles, para la pagina publica. */
    public List<CustomSection> listarVisibles() {
        return repository.findByVisibleTrueOrderByOrdenAsc();
    }

    public CustomSection buscar(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public void crearTexto(String titulo, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        CustomSection s = new CustomSection();
        s.setTipo("texto");
        s.setTitulo(titulo != null ? recortar(titulo.trim(), MAX_TITULO) : null);
        s.setTexto(recortar(texto.trim(), MAX_TEXTO));
        s.setAncho(32);
        posicionInicial(s);
        repository.save(s);
    }

    /** Devuelve false (sin guardar nada) si el archivo no es una imagen real. */
    @Transactional
    public boolean crearImagen(byte[] contenido) {
        if (!validator.validarTamano(contenido).isValido()) {
            return false;
        }
        MaterialFileValidator.Resultado tipo = validator.validarTipo(contenido);
        if (!tipo.isValido() || !tipo.getTipoDetectado().startsWith("image/")) {
            return false;
        }
        CustomSection s = new CustomSection();
        s.setTipo("imagen");
        s.setImagenData(contenido);
        s.setImagenContentType(tipo.getTipoDetectado());
        s.setImagenVersion(System.currentTimeMillis());
        s.setAncho(22);
        posicionInicial(s);
        repository.save(s);
        return true;
    }

    /** Posicion escalonada segun cuantos elementos ya existen, para que uno nuevo no tape a otro. */
    private void posicionInicial(CustomSection s) {
        long cantidad = repository.count();
        s.setPosX(6 + (cantidad % 5) * 16);
        s.setPosY(6 + (cantidad % 4) * 14);
        s.setRotacion(0);
        s.setVisible(true);
        int maxOrden = repository.findAllByOrderByOrdenAsc().stream()
                .mapToInt(CustomSection::getOrden).max().orElse(-1);
        s.setOrden(maxOrden + 1);
    }

    /** Actualiza titulo y texto juntos (usado por el formulario del panel clasico). */
    @Transactional
    public void actualizar(Long id, String titulo, String texto) {
        repository.findById(id).ifPresent(s -> {
            if (titulo != null && !titulo.isBlank()) {
                s.setTitulo(recortar(titulo.trim(), MAX_TITULO));
            }
            if (texto != null && !texto.isBlank()) {
                s.setTexto(recortar(texto.trim(), MAX_TEXTO));
            }
            repository.save(s);
        });
    }

    /** Actualiza solo el titulo (usado por el editor visual, que edita un campo a la vez). */
    @Transactional
    public void actualizarTitulo(Long id, String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return;
        }
        repository.findById(id).ifPresent(s -> {
            s.setTitulo(recortar(titulo.trim(), MAX_TITULO));
            repository.save(s);
        });
    }

    /** Actualiza solo el texto (usado por el editor visual, que edita un campo a la vez). */
    @Transactional
    public void actualizarTexto(Long id, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        repository.findById(id).ifPresent(s -> {
            s.setTexto(recortar(texto.trim(), MAX_TEXTO));
            repository.save(s);
        });
    }

    /** Guarda la nueva posicion/rotacion/ancho tras arrastrar o rotar el elemento en el lienzo. */
    @Transactional
    public boolean actualizarPosicion(Long id, double posX, double posY, double rotacion, double ancho) {
        return repository.findById(id).map(s -> {
            s.setPosX(limitar(posX, POS_MIN, POS_MAX));
            s.setPosY(limitar(posY, POS_MIN, POS_MAX));
            s.setRotacion(limitar(rotacion, -ROTACION_MAX, ROTACION_MAX));
            s.setAncho(limitar(ancho, ANCHO_MIN, ANCHO_MAX));
            repository.save(s);
            return true;
        }).orElse(false);
    }

    @Transactional
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void alternarVisible(Long id) {
        repository.findById(id).ifPresent(s -> {
            s.setVisible(!s.isVisible());
            repository.save(s);
        });
    }

    private static double limitar(double v, double min, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }
        return Math.max(min, Math.min(max, v));
    }

    private static String recortar(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
