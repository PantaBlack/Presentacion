package com.imfundokahle.service;

import com.imfundokahle.model.ContentText;
import com.imfundokahle.repository.ContentTextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Textos editables de la pagina publica de inicio. Deliberadamente NO incluye
 * claves que se reutilizan en otras partes de la app (ej. "lang.spanish",
 * "stats.teachers", usadas tambien en paneles internos): si esas se pudieran
 * editar aqui, un cambio pensado "solo para el home" cambiaria tambien texto
 * de las pantallas de admin/alumno/profesor sin que el editor se de cuenta.
 */
@Service
public class ContentTextService {

    /** Un campo de texto editable. "grupo" es opcional: agrupa visualmente varios
     *  campos relacionados dentro de una seccion larga (ej. los 3 planes de precios). */
    public static class Campo {
        private final String clave;
        private final String etiqueta;
        private final String grupo;

        public Campo(String clave, String etiqueta) {
            this(clave, etiqueta, null);
        }

        public Campo(String clave, String etiqueta, String grupo) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.grupo = grupo;
        }

        public String getClave() { return clave; }
        public String getEtiqueta() { return etiqueta; }
        public String getGrupo() { return grupo; }
    }

    /** Una seccion del home con sus campos editables. */
    public static class Seccion {
        private final String titulo;
        private final List<Campo> campos;

        public Seccion(String titulo, List<Campo> campos) {
            this.titulo = titulo;
            this.campos = campos;
        }

        public String getTitulo() { return titulo; }
        public List<Campo> getCampos() { return campos; }
    }

    private static final List<Seccion> SECCIONES = List.of(
            new Seccion("🎯 Portada (hero)", List.of(
                    new Campo("landing.hero.badge", "Frase pequena arriba del titulo"),
                    new Campo("landing.hero.title", "Titulo grande"),
                    new Campo("landing.hero.subtitle", "Texto debajo del titulo"),
                    new Campo("landing.hero.bullet.1", "Lista - linea 1"),
                    new Campo("landing.hero.bullet.2", "Lista - linea 2"),
                    new Campo("landing.hero.bullet.3", "Lista - linea 3"),
                    new Campo("landing.hero.bullet.4", "Lista - linea 4"),
                    new Campo("landing.hero.cta.primary", "Texto del boton naranja"),
                    new Campo("landing.hero.cta.secondary", "Texto del boton blanco")
            )),
            new Seccion("🎉 Banner de promocion", List.of(
                    new Campo("landing.promo.title", "Titulo del banner"),
                    new Campo("landing.promo.subtitle", "Texto debajo del titulo"),
                    new Campo("landing.promo.perk.1", "Beneficio 1"),
                    new Campo("landing.promo.perk.2", "Beneficio 2"),
                    new Campo("landing.promo.perk.3", "Beneficio 3"),
                    new Campo("landing.promo.cta", "Texto del boton"),
                    new Campo("landing.promo.countdown.label", "Frase sobre el contador de tiempo")
            )),
            new Seccion("🔄 Como funciona", List.of(
                    new Campo("landing.how.title", "Titulo de la seccion"),
                    new Campo("landing.how.lead", "Texto debajo del titulo"),
                    new Campo("landing.how.step1.title", "Titulo", "Paso 1"),
                    new Campo("landing.how.step1.desc", "Descripcion", "Paso 1"),
                    new Campo("landing.how.step2.title", "Titulo", "Paso 2"),
                    new Campo("landing.how.step2.desc", "Descripcion", "Paso 2"),
                    new Campo("landing.how.step3.title", "Titulo", "Paso 3"),
                    new Campo("landing.how.step3.desc", "Descripcion", "Paso 3"),
                    new Campo("landing.how.step4.title", "Titulo", "Paso 4"),
                    new Campo("landing.how.step4.desc", "Descripcion", "Paso 4")
            )),
            new Seccion("🌐 Idiomas / cursos", List.of(
                    new Campo("landing.languages.title", "Titulo de la seccion"),
                    new Campo("landing.languages.lead", "Texto debajo del titulo"),
                    new Campo("landing.lang.spanish.desc", "Descripcion", "Espanol"),
                    new Campo("landing.lang.french.desc", "Descripcion", "Frances"),
                    new Campo("landing.lang.english.desc", "Descripcion", "Ingles"),
                    new Campo("landing.lang.cta", "Texto del boton (las 3 tarjetas)")
            )),
            new Seccion("⭐ Por que elegirnos", List.of(
                    new Campo("landing.features.title", "Titulo de la seccion"),
                    new Campo("landing.features.lead", "Texto debajo del titulo"),
                    new Campo("feature.1.title", "Titulo", "Tarjeta 1"),
                    new Campo("feature.1.desc", "Descripcion", "Tarjeta 1"),
                    new Campo("feature.2.title", "Titulo", "Tarjeta 2"),
                    new Campo("feature.2.desc", "Descripcion", "Tarjeta 2"),
                    new Campo("feature.3.title", "Titulo", "Tarjeta 3"),
                    new Campo("feature.3.desc", "Descripcion", "Tarjeta 3"),
                    new Campo("feature.4.title", "Titulo", "Tarjeta 4"),
                    new Campo("feature.4.desc", "Descripcion", "Tarjeta 4")
            )),
            new Seccion("💳 Precios", List.of(
                    new Campo("landing.pricing.title", "Titulo de la seccion"),
                    new Campo("landing.pricing.lead", "Texto debajo del titulo"),
                    new Campo("pricing.cta", "Texto del boton (los 3 planes)"),
                    new Campo("pricing.basic.name", "Nombre del plan", "Plan Basico"),
                    new Campo("pricing.basic.desc", "Descripcion corta", "Plan Basico"),
                    new Campo("pricing.basic.price", "Precio (ej. S/ 99)", "Plan Basico"),
                    new Campo("pricing.basic.period", "Periodo (ej. /mes)", "Plan Basico"),
                    new Campo("pricing.basic.f1", "Caracteristica 1", "Plan Basico"),
                    new Campo("pricing.basic.f2", "Caracteristica 2", "Plan Basico"),
                    new Campo("pricing.basic.f3", "Caracteristica 3", "Plan Basico"),
                    new Campo("pricing.basic.f4", "Caracteristica 4", "Plan Basico"),
                    new Campo("pricing.standard.badge", "Insignia (ej. Mas popular)", "Plan Estandar"),
                    new Campo("pricing.standard.name", "Nombre del plan", "Plan Estandar"),
                    new Campo("pricing.standard.desc", "Descripcion corta", "Plan Estandar"),
                    new Campo("pricing.standard.price", "Precio", "Plan Estandar"),
                    new Campo("pricing.standard.period", "Periodo", "Plan Estandar"),
                    new Campo("pricing.standard.f1", "Caracteristica 1", "Plan Estandar"),
                    new Campo("pricing.standard.f2", "Caracteristica 2", "Plan Estandar"),
                    new Campo("pricing.standard.f3", "Caracteristica 3", "Plan Estandar"),
                    new Campo("pricing.standard.f4", "Caracteristica 4", "Plan Estandar"),
                    new Campo("pricing.premium.name", "Nombre del plan", "Plan Premium"),
                    new Campo("pricing.premium.desc", "Descripcion corta", "Plan Premium"),
                    new Campo("pricing.premium.price", "Precio", "Plan Premium"),
                    new Campo("pricing.premium.period", "Periodo", "Plan Premium"),
                    new Campo("pricing.premium.f1", "Caracteristica 1", "Plan Premium"),
                    new Campo("pricing.premium.f2", "Caracteristica 2", "Plan Premium"),
                    new Campo("pricing.premium.f3", "Caracteristica 3", "Plan Premium"),
                    new Campo("pricing.premium.f4", "Caracteristica 4", "Plan Premium")
            )),
            new Seccion("📚 Nuestros libros", List.of(
                    new Campo("landing.books.title", "Titulo de la seccion"),
                    new Campo("landing.books.lead", "Texto debajo del titulo"),
                    new Campo("landing.books.1.caption", "Descripcion", "Viva Latinoamerica 1"),
                    new Campo("landing.books.2.caption", "Descripcion", "Viva Latinoamerica 2"),
                    new Campo("landing.books.3.caption", "Descripcion", "Viva Latinoamerica 3"),
                    new Campo("landing.books.palabras.caption", "Descripcion", "Palabras y expresiones"),
                    new Campo("landing.books.survival.caption", "Descripcion", "Survival Spanish")
            )),
            new Seccion("💬 Testimonios", List.of(
                    new Campo("landing.testimonials.title", "Titulo de la seccion"),
                    new Campo("testimonial.1.quote", "Lo que dice", "Testimonio 1"),
                    new Campo("testimonial.1.who", "Nombre de la persona", "Testimonio 1"),
                    new Campo("testimonial.2.quote", "Lo que dice", "Testimonio 2"),
                    new Campo("testimonial.2.who", "Nombre de la persona", "Testimonio 2"),
                    new Campo("testimonial.3.quote", "Lo que dice", "Testimonio 3"),
                    new Campo("testimonial.3.who", "Nombre de la persona", "Testimonio 3")
            )),
            new Seccion("📋 Pie de pagina", List.of(
                    new Campo("footer.about", "Texto sobre la empresa"),
                    new Campo("footer.nav.title", "Titulo de la columna 'Enlaces'"),
                    new Campo("footer.languages.title", "Titulo de la columna 'Idiomas'"),
                    new Campo("footer.contact.title", "Titulo de la columna 'Contacto'"),
                    new Campo("footer.rights", "Texto de derechos reservados")
            )),
            new Seccion("🧭 Menu de navegacion", List.of(
                    new Campo("nav.home", "Inicio"),
                    new Campo("nav.courses", "Cursos"),
                    new Campo("nav.methodology", "Metodologia"),
                    new Campo("nav.pricing", "Precios"),
                    new Campo("nav.testimonials", "Testimonios")
            )),
            new Seccion("🧑‍🏫 Panel del Profesor", List.of(
                    new Campo("teacher.portal", "Frase pequena arriba del titulo"),
                    new Campo("teacher.dashboard.subtitle", "Texto debajo del saludo")
            )),
            new Seccion("🎓 Panel del Alumno", List.of(
                    new Campo("student.portal", "Frase pequena arriba del titulo"),
                    new Campo("student.dashboard.subtitle", "Texto debajo del saludo")
            )),
            new Seccion("🌱 Panel del Practicante", List.of(
                    new Campo("practicante.portal", "Frase pequena arriba del titulo"),
                    new Campo("practicante.dashboard.subtitle", "Texto debajo del saludo")
            )),
            new Seccion("🛡️ Panel del Administrador", List.of(
                    new Campo("dash.eyebrow.admin", "Frase pequena arriba del titulo"),
                    new Campo("admin.dashboard.title", "Titulo del panel"),
                    new Campo("admin.dashboard.subtitle", "Texto debajo del titulo")
            ))
    );

    private final ContentTextRepository repository;

    public ContentTextService(ContentTextRepository repository) {
        this.repository = repository;
    }

    public List<Seccion> getSecciones() {
        return SECCIONES;
    }

    /** Todas las claves editables validas (para rechazar cualquier otra en el guardado). */
    public boolean esClaveEditable(String key) {
        return SECCIONES.stream().flatMap(s -> s.getCampos().stream()).anyMatch(c -> c.getClave().equals(key));
    }

    /**
     * Valor guardado para esa clave e idioma ("es"/"fr"/"en"), si el admin lo
     * sobreescribio. El chequeo en memoria contra la lista editable evita una
     * consulta a la base de datos por cada uno de los +900 textos de la app
     * que jamas van a tener una sobreescritura (esto se llama en CADA
     * resolucion de mensaje, en cada pagina).
     */
    public Optional<String> getOverride(String messageKey, String idioma) {
        if (!esClaveEditable(messageKey)) {
            return Optional.empty();
        }
        return repository.findByMessageKey(messageKey)
                .map(ct -> ct.getValorParaIdioma(idioma))
                .filter(v -> v != null && !v.isBlank());
    }

    public Map<String, ContentText> getTodosComoMapa() {
        Map<String, ContentText> mapa = new LinkedHashMap<>();
        for (ContentText ct : repository.findAll()) {
            mapa.put(ct.getMessageKey(), ct);
        }
        return mapa;
    }

    /** Guarda (o borra si vienen vacios los 3) el valor de una clave. Ignora claves fuera de la lista editable. */
    @Transactional
    public void guardar(String messageKey, String es, String fr, String en) {
        if (!esClaveEditable(messageKey)) {
            return;
        }
        ContentText ct = repository.findByMessageKey(messageKey).orElseGet(() -> {
            ContentText nuevo = new ContentText();
            nuevo.setMessageKey(messageKey);
            return nuevo;
        });
        ct.setValorEs(vacioANull(es));
        ct.setValorFr(vacioANull(fr));
        ct.setValorEn(vacioANull(en));
        repository.save(ct);
    }

    /**
     * Guarda el valor de UN solo idioma sin tocar los otros dos (a diferencia
     * de {@link #guardar}, que siempre reemplaza los 3). Lo usa el editor
     * visual, que solo conoce el idioma que el admin tiene abierto en pantalla.
     */
    @Transactional
    public void guardarUnIdioma(String messageKey, String idioma, String valor) {
        if (!esClaveEditable(messageKey)) {
            return;
        }
        ContentText ct = repository.findByMessageKey(messageKey).orElseGet(() -> {
            ContentText nuevo = new ContentText();
            nuevo.setMessageKey(messageKey);
            return nuevo;
        });
        String v = vacioANull(valor);
        switch (idioma) {
            case "fr" -> ct.setValorFr(v);
            case "en" -> ct.setValorEn(v);
            default -> ct.setValorEs(v);
        }
        repository.save(ct);
    }

    private static String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
