package com.imfundokahle.config;

import com.imfundokahle.service.ContentTextService;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * MessageSource normal (lee messages*.properties), pero antes de resolver
 * cualquier clave consulta si el admin la sobreescribio desde el panel
 * "Contenido del sitio". Si no hay sobreescritura para esa clave/idioma, cae
 * exactamente en el comportamiento de siempre.
 * <p>
 * Solo las claves listadas en {@link ContentTextService} pueden tener una
 * sobreescritura guardada (ver esa clase para el porque), asi que esto nunca
 * afecta el resto de las +900 claves de la app.
 */
public class DbAwareMessageSource extends ReloadableResourceBundleMessageSource {

    private final ContentTextService contentTextService;

    public DbAwareMessageSource(ContentTextService contentTextService) {
        this.contentTextService = contentTextService;
    }

    /**
     * Spring llama a este metodo (sin pasar por resolveCode) para CUALQUIER
     * mensaje sin parametros "{0}" - que es el caso de absolutamente todos los
     * textos del home. Si solo se sobreescribe resolveCode(), como en un primer
     * intento, esta sobreescritura nunca se ejecuta y el panel de admin no
     * cambiaria nada de verdad en la pagina.
     */
    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        String override = contentTextService.getOverride(code, locale.getLanguage()).orElse(null);
        if (override != null) {
            return override;
        }
        return super.resolveCodeWithoutArguments(code, locale);
    }

    /** Cubre el caso (no usado hoy por ningun texto del home) de un mensaje CON parametros "{0}". */
    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String override = contentTextService.getOverride(code, locale.getLanguage()).orElse(null);
        if (override != null) {
            // MessageFormat trata la comilla simple como caracter de escape: el admin
            // puede escribir con normalidad (ej. "no te preocupes'") sin que se rompa.
            return new MessageFormat(override.replace("'", "''"), locale);
        }
        return super.resolveCode(code, locale);
    }
}
