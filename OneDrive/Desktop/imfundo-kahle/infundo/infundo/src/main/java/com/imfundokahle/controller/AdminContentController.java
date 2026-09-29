package com.imfundokahle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfundokahle.model.ContentImage;
import com.imfundokahle.model.PromoTema;
import com.imfundokahle.model.Role;
import com.imfundokahle.service.ContentImageService;
import com.imfundokahle.service.ContentTextService;
import com.imfundokahle.service.CustomSectionService;
import com.imfundokahle.service.MaterialService;
import com.imfundokahle.service.PromoBannerService;
import com.imfundokahle.service.ScheduleService;
import com.imfundokahle.service.UserService;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panel de admin para editar, sin tocar codigo, los textos e imagenes de la
 * pagina publica de inicio. Ver {@link ContentTextService} para el porque de
 * la lista de claves acotada (no cualquier texto de la app es editable aqui).
 */
@Controller
@RequestMapping("/admin/contenido")
public class AdminContentController {

    /** Vista de un campo con su valor actual (sobreescrito o el de fabrica) en los 3 idiomas. */
    public static class CampoConValor {
        private final String clave, etiqueta, grupo, es, fr, en;
        private final boolean nuevoGrupo;

        public CampoConValor(String clave, String etiqueta, String grupo, boolean nuevoGrupo,
                             String es, String fr, String en) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.grupo = grupo;
            this.nuevoGrupo = nuevoGrupo;
            this.es = es;
            this.fr = fr;
            this.en = en;
        }

        public String getClave() { return clave; }
        public String getEtiqueta() { return etiqueta; }
        public String getGrupo() { return grupo; }
        public boolean isNuevoGrupo() { return nuevoGrupo; }
        public String getEs() { return es; }
        public String getFr() { return fr; }
        public String getEn() { return en; }
    }

    public static class SeccionConValores {
        private final String titulo;
        private final List<CampoConValor> campos;

        public SeccionConValores(String titulo, List<CampoConValor> campos) {
            this.titulo = titulo;
            this.campos = campos;
        }

        public String getTitulo() { return titulo; }
        public List<CampoConValor> getCampos() { return campos; }
    }

    public static class ImagenConDatos {
        private final String clave, etiqueta;
        private final boolean tieneImagen;
        private final boolean opcional;
        private final long version;

        public ImagenConDatos(String clave, String etiqueta, boolean tieneImagen, boolean opcional, long version) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.tieneImagen = tieneImagen;
            this.opcional = opcional;
            this.version = version;
        }

        public String getClave() { return clave; }
        public String getEtiqueta() { return etiqueta; }
        public boolean isTieneImagen() { return tieneImagen; }
        public boolean isOpcional() { return opcional; }
        public long getVersion() { return version; }
    }

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es");
    private static final Locale LOCALE_FR = Locale.forLanguageTag("fr");
    private static final Locale LOCALE_EN = Locale.forLanguageTag("en");

    private final ContentTextService contentTextService;
    private final ContentImageService contentImageService;
    private final PromoBannerService promoBannerService;
    private final MessageSource messageSource;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final MaterialService materialService;
    private final ObjectMapper objectMapper;
    private final CustomSectionService customSectionService;
    private final TeacherController teacherController;
    private final StudentController studentController;
    private final PracticanteController practicanteController;

    public AdminContentController(ContentTextService contentTextService,
                                  ContentImageService contentImageService,
                                  PromoBannerService promoBannerService,
                                  MessageSource messageSource,
                                  UserService userService,
                                  ScheduleService scheduleService,
                                  MaterialService materialService,
                                  ObjectMapper objectMapper,
                                  CustomSectionService customSectionService,
                                  TeacherController teacherController,
                                  StudentController studentController,
                                  PracticanteController practicanteController) {
        this.contentTextService = contentTextService;
        this.contentImageService = contentImageService;
        this.promoBannerService = promoBannerService;
        this.messageSource = messageSource;
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.materialService = materialService;
        this.objectMapper = objectMapper;
        this.customSectionService = customSectionService;
        this.teacherController = teacherController;
        this.studentController = studentController;
        this.practicanteController = practicanteController;
    }

    /**
     * Editor visual: sirve la MISMA plantilla del home publico ("index"), pero
     * con modoEdicion=true para que se inyecten los ganchos de edicion en
     * linea (ver el bloque final de index.html). Asi el admin ve exactamente
     * la pagina real, no una maqueta aparte que se pueda desincronizar.
     */
    @GetMapping("/editor")
    public String editor(Model model) {
        model.addAttribute("totalTeachers", userService.countByRole(Role.TEACHER));
        model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
        model.addAttribute("totalClasses", scheduleService.countActiveSchedules());
        model.addAttribute("totalMaterials", materialService.countAll());
        model.addAttribute("promoTema", promoBannerService.getTema().name());
        model.addAttribute("imgV", contentImageService.getVersiones());
        model.addAttribute("seccionesPersonalizadas", customSectionService.listarVisibles());
        model.addAttribute("modoEdicion", true);
        return "index";
    }

    /**
     * Igual que {@link #editor}, pero para el panel de un profesor: el admin no puede
     * entrar a /teacher/dashboard (esa ruta es solo para profesores), asi que aca se
     * arma esa misma pantalla con un profesor real cualquiera (los datos que se ven no
     * importan para editar textos e imagenes, solo hace falta que la pagina no truene).
     * Reutiliza la construccion del modelo de TeacherController para no duplicarla.
     */
    @GetMapping("/editor/profesor")
    public String editorProfesor(Model model, RedirectAttributes ra) {
        return userService.findByRole(Role.TEACHER).stream().findFirst()
                .map(teacher -> {
                    teacherController.construirModeloDashboard(teacher, model);
                    model.addAttribute("modoEdicion", true);
                    return "teacher/dashboard";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("errorKey", "admin.contenido.editor.sinUsuarios");
                    return "redirect:/admin/contenido";
                });
    }

    /** Igual que {@link #editorProfesor}, para el panel de un alumno. */
    @GetMapping("/editor/alumno")
    public String editorAlumno(Model model, RedirectAttributes ra) {
        return userService.findByRole(Role.STUDENT).stream().findFirst()
                .map(student -> {
                    studentController.construirModeloDashboard(student, model);
                    model.addAttribute("modoEdicion", true);
                    return "student/dashboard";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("errorKey", "admin.contenido.editor.sinUsuarios");
                    return "redirect:/admin/contenido";
                });
    }

    /** Igual que {@link #editorProfesor}, para el panel de un practicante. */
    @GetMapping("/editor/practicante")
    public String editorPracticante(Model model, RedirectAttributes ra) {
        return userService.findByRole(Role.PRACTICANTE).stream().findFirst()
                .map(practicante -> {
                    practicanteController.construirModeloDashboard(practicante, model);
                    model.addAttribute("modoEdicion", true);
                    return "practicante/dashboard";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("errorKey", "admin.contenido.editor.sinUsuarios");
                    return "redirect:/admin/contenido";
                });
    }

    /** Payload del editor visual: solo el idioma que el admin tiene abierto, y los textos que cambio. */
    public static class VisualPayload {
        private String idioma;
        private Map<String, String> textos;

        public String getIdioma() { return idioma; }
        public void setIdioma(String idioma) { this.idioma = idioma; }
        public Map<String, String> getTextos() { return textos; }
        public void setTextos(Map<String, String> textos) { this.textos = textos; }
    }

    /** Igual al limite de la columna content_text.valor* (@Column(length=2000)): recortamos antes de guardar
     *  para nunca dejar que un texto largo pegado por el admin tumbe el guardado con un error de base de datos. */
    private static final int MAX_LARGO_TEXTO = 2000;

    /**
     * Guarda de una sola vez los textos editados en linea y las imagenes
     * reemplazadas desde el editor visual. Las imagenes viajan como partes
     * multipart nombradas "img__<clave>"; todo lo demas (JSON de textos) viaja
     * en la parte "datos". Cada clave se valida igual que en los endpoints
     * clasicos (ver ContentTextService/ContentImageService), asi que un valor
     * fuera de la lista editable simplemente se ignora.
     *
     * Nunca deja que UN campo o UNA imagen con problemas tumbe el resto del
     * guardado (ni la peticion completa): cada uno se procesa en su propio
     * try/catch y la respuesta siempre es JSON, incluso si algo salio mal.
     */
    @PostMapping(value = "/guardar-visual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarVisual(@RequestParam("datos") String datosJson,
                                                              MultipartHttpServletRequest request) {
        VisualPayload payload;
        try {
            payload = objectMapper.readValue(datosJson, VisualPayload.class);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false));
        }

        int textosGuardados = 0;
        if (payload.getTextos() != null) {
            String idioma = payload.getIdioma();
            idioma = ("fr".equals(idioma) || "en".equals(idioma)) ? idioma : "es";
            for (Map.Entry<String, String> entrada : payload.getTextos().entrySet()) {
                try {
                    String clave = entrada.getKey();
                    String valor = entrada.getValue();
                    if (valor != null && valor.length() > MAX_LARGO_TEXTO) {
                        valor = valor.substring(0, MAX_LARGO_TEXTO);
                    }
                    // Las secciones personalizadas (agregadas por el admin, no son parte del
                    // diseno fijo) viajan con claves "cs-titulo-<id>"/"cs-texto-<id>" en vez de
                    // una clave de mensaje; se guardan aparte, en su propia tabla.
                    if (clave.startsWith("cs-titulo-")) {
                        customSectionService.actualizarTitulo(Long.valueOf(clave.substring(10)), valor);
                    } else if (clave.startsWith("cs-texto-")) {
                        customSectionService.actualizarTexto(Long.valueOf(clave.substring(9)), valor);
                    } else {
                        contentTextService.guardarUnIdioma(clave, idioma, valor);
                    }
                    textosGuardados++;
                } catch (RuntimeException ex) {
                    // Esta clave no se guardo; seguimos con las demas.
                }
            }
        }

        int imagenesGuardadas = 0;
        int imagenesRechazadas = 0;
        for (Map.Entry<String, MultipartFile> entrada : request.getFileMap().entrySet()) {
            String nombreCampo = entrada.getKey();
            if (!nombreCampo.startsWith("img__")) {
                continue;
            }
            MultipartFile archivo = entrada.getValue();
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            try {
                if (contentImageService.reemplazar(nombreCampo.substring(5), archivo.getBytes())) {
                    imagenesGuardadas++;
                } else {
                    imagenesRechazadas++;
                }
            } catch (IOException | RuntimeException ex) {
                imagenesRechazadas++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "textos", textosGuardados,
                "imagenes", imagenesGuardadas,
                "imagenesRechazadas", imagenesRechazadas
        ));
    }

    @GetMapping
    public String panel(Model model) {
        List<SeccionConValores> secciones = new ArrayList<>();
        for (ContentTextService.Seccion seccion : contentTextService.getSecciones()) {
            List<CampoConValor> campos = new ArrayList<>();
            String grupoAnterior = null;
            for (ContentTextService.Campo campo : seccion.getCampos()) {
                String grupo = campo.getGrupo();
                boolean nuevoGrupo = grupo != null && !grupo.equals(grupoAnterior);
                grupoAnterior = grupo;
                campos.add(new CampoConValor(
                        campo.getClave(),
                        campo.getEtiqueta(),
                        grupo,
                        nuevoGrupo,
                        valorActual(campo.getClave(), LOCALE_ES),
                        valorActual(campo.getClave(), LOCALE_FR),
                        valorActual(campo.getClave(), LOCALE_EN)
                ));
            }
            secciones.add(new SeccionConValores(seccion.getTitulo(), campos));
        }
        model.addAttribute("secciones", secciones);

        Map<String, ContentImage> imagenesGuardadas = contentImageService.getTodasComoMapa();
        List<ImagenConDatos> imagenes = ContentImageService.IMAGENES.stream()
                .map(def -> {
                    ContentImage guardada = imagenesGuardadas.get(def.getClave());
                    long version = guardada != null ? guardada.getVersion() : 0L;
                    return new ImagenConDatos(def.getClave(), def.getEtiqueta(), guardada != null, def.isOpcional(), version);
                })
                .toList();
        model.addAttribute("imagenes", imagenes);

        model.addAttribute("temaActual", promoBannerService.getTema().name());
        model.addAttribute("temas", PromoTema.values());

        model.addAttribute("seccionesPersonalizadas", customSectionService.listarTodas());

        return "admin/contenido";
    }

    @PostMapping("/tema")
    public String guardarTema(@RequestParam String tema, RedirectAttributes ra) {
        try {
            promoBannerService.setTema(PromoTema.valueOf(tema));
            ra.addFlashAttribute("successKey", "admin.contenido.guardado");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorKey", "admin.contenido.tema.invalido");
        }
        return "redirect:/admin/contenido";
    }

    /** El valor a mostrar en el formulario: lo que ya se sobreescribio, o el texto de fabrica de ese idioma. */
    private String valorActual(String clave, Locale locale) {
        return messageSource.getMessage(clave, null, clave, locale);
    }

    @PostMapping("/texto")
    public String guardarTextos(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        for (ContentTextService.Seccion seccion : contentTextService.getSecciones()) {
            for (ContentTextService.Campo campo : seccion.getCampos()) {
                String clave = campo.getClave();
                contentTextService.guardar(clave,
                        params.get("es__" + clave),
                        params.get("fr__" + clave),
                        params.get("en__" + clave));
            }
        }
        ra.addFlashAttribute("successKey", "admin.contenido.guardado");
        return "redirect:/admin/contenido";
    }

    @PostMapping("/imagen/{key}")
    public String reemplazarImagen(@PathVariable String key, @RequestParam MultipartFile archivo,
                                   RedirectAttributes ra) {
        if (archivo == null || archivo.isEmpty()) {
            ra.addFlashAttribute("errorKey", "admin.contenido.imagen.vacia");
            return "redirect:/admin/contenido";
        }
        try {
            boolean ok = contentImageService.reemplazar(key, archivo.getBytes());
            ra.addFlashAttribute(ok ? "successKey" : "errorKey",
                    ok ? "admin.contenido.imagen.guardada" : "admin.contenido.imagen.invalida");
        } catch (IOException e) {
            ra.addFlashAttribute("errorKey", "admin.contenido.imagen.invalida");
        }
        return "redirect:/admin/contenido";
    }

    /** Quita una imagen opcional (ej. el fondo de portada) y vuelve al estilo por defecto. */
    @PostMapping("/imagen/{key}/eliminar")
    public String eliminarImagen(@PathVariable String key, RedirectAttributes ra) {
        boolean ok = contentImageService.eliminar(key);
        ra.addFlashAttribute(ok ? "successKey" : "errorKey",
                ok ? "admin.contenido.imagen.quitada" : "admin.contenido.imagen.noQuitable");
        return "redirect:/admin/contenido";
    }

    // ---- Zona de diseno libre: elementos de texto/imagen que el admin agrega y puede
    // arrastrar y rotar dentro del lienzo, ademas de editar los campos fijos del diseno ----

    @PostMapping("/secciones")
    public String crearSeccion(@RequestParam(required = false) String titulo,
                               @RequestParam String texto, RedirectAttributes ra) {
        customSectionService.crearTexto(titulo, texto);
        ra.addFlashAttribute("successKey", "admin.contenido.guardado");
        return "redirect:/admin/contenido";
    }

    @PostMapping(value = "/secciones/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String crearSeccionImagen(@RequestParam MultipartFile archivo, RedirectAttributes ra) {
        if (archivo == null || archivo.isEmpty()) {
            ra.addFlashAttribute("errorKey", "admin.contenido.imagen.vacia");
            return "redirect:/admin/contenido";
        }
        try {
            boolean ok = customSectionService.crearImagen(archivo.getBytes());
            ra.addFlashAttribute(ok ? "successKey" : "errorKey",
                    ok ? "admin.contenido.guardado" : "admin.contenido.imagen.invalida");
        } catch (IOException e) {
            ra.addFlashAttribute("errorKey", "admin.contenido.imagen.invalida");
        }
        return "redirect:/admin/contenido";
    }

    @PostMapping("/secciones/{id}")
    public String actualizarSeccion(@PathVariable Long id, @RequestParam String titulo,
                                    @RequestParam String texto, RedirectAttributes ra) {
        customSectionService.actualizar(id, titulo, texto);
        ra.addFlashAttribute("successKey", "admin.contenido.guardado");
        return "redirect:/admin/contenido";
    }

    @PostMapping("/secciones/{id}/eliminar")
    public String eliminarSeccion(@PathVariable Long id, RedirectAttributes ra) {
        customSectionService.eliminar(id);
        ra.addFlashAttribute("successKey", "admin.contenido.guardado");
        return "redirect:/admin/contenido";
    }

    @PostMapping("/secciones/{id}/visibilidad")
    public String alternarVisibilidadSeccion(@PathVariable Long id, RedirectAttributes ra) {
        customSectionService.alternarVisible(id);
        ra.addFlashAttribute("successKey", "admin.contenido.guardado");
        return "redirect:/admin/contenido";
    }

    /** Posicion dentro del lienzo (arrastrar/rotar): AJAX, se guarda al instante al soltar el elemento. */
    public static class PosicionPayload {
        private double posX, posY, rotacion, ancho;
        public double getPosX() { return posX; }
        public void setPosX(double posX) { this.posX = posX; }
        public double getPosY() { return posY; }
        public void setPosY(double posY) { this.posY = posY; }
        public double getRotacion() { return rotacion; }
        public void setRotacion(double rotacion) { this.rotacion = rotacion; }
        public double getAncho() { return ancho; }
        public void setAncho(double ancho) { this.ancho = ancho; }
    }

    @PostMapping("/secciones/{id}/posicion")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarPosicionSeccion(@PathVariable Long id,
                                                                         @RequestBody PosicionPayload payload) {
        boolean ok = customSectionService.actualizarPosicion(id, payload.getPosX(), payload.getPosY(),
                payload.getRotacion(), payload.getAncho());
        return ResponseEntity.ok(Map.of("ok", ok));
    }

    /** El archivo supera el limite configurado (spring.servlet.multipart.max-file-size). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande(RedirectAttributes ra) {
        ra.addFlashAttribute("errorKey", "admin.contenido.imagen.pesada");
        return "redirect:/admin/contenido";
    }
}
