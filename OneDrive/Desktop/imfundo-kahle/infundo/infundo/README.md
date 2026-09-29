# 🌍 Viva Idiomas — Centro de Idiomas

Sistema web educativo completo para el centro de idiomas **Viva Idiomas**, desarrollado en **Java Spring Boot MVC** con un diseño moderno azul marino/naranja, fiel a la identidad de marca.

La plataforma ofrece tres portales independientes (Administrador, Profesor y Alumno) además de un portal de Practicante, gestión de horarios, propuestas, materiales educativos e inscripciones a clases, con soporte multiidioma (Español / Francés / Inglés), recuperación de contraseña por correo y un editor visual para que el administrador cambie el contenido del sitio sin tocar código.

---

## ✨ Características principales

- **Landing page pública** con hero, banner promocional con cuenta regresiva, sección "¿Cómo funciona?", idiomas ofrecidos, colección de libros, características, planes/precios, estadísticas animadas y testimonios.
- **Editor visual del sitio** (`/admin/contenido/editor`): el administrador edita textos e imágenes haciendo clic directo sobre la página real, agrega bloques de texto/imagen nuevos y los arrastra/rota donde quiera, y puede poner una foto o GIF de fondo en la portada, el banner promocional o la sección de estadísticas — todo sin escribir código.
- **Temas de temporada**: un listón decorativo en la esquina (Navidad, Halloween, invierno, descuentos) que el administrador activa desde el panel.
- **Autenticación segura** con Spring Security + BCrypt, **Google reCAPTCHA v2** en login/registro/recuperar contraseña, y bloqueo temporal de cuenta tras varios intentos fallidos.
- **Recuperación de contraseña por correo real** (Gmail SMTP): código de 6 dígitos con expiración de 15 minutos, sin guardar nada sensible en la base de datos.
- **Auto-registro restringido**: solo con correo `@gmail.com` y solo como Alumno o Profesor. Los roles Practicante y Administrador los asigna un administrador desde `/admin/users`; nunca se obtienen registrándose.
- **Portal Administrador**: estadísticas globales, gestión de propuestas, horarios de todos los profesores, materiales, gestión de usuarios/roles, control de horas/actividades del equipo y el editor de contenido del sitio.
- **Portal Profesor**: panel personal, gestión de horarios semanal, creación de propuestas y subida de materiales.
- **Portal Alumno**: panel, exploración e inscripción a clases por idioma, materiales y edición de perfil.
- **Portal Practicante**: panel propio para seguimiento de actividades y horas.
- **Multiidioma (i18n)**: Español (por defecto), Francés e Inglés, conmutables desde la barra superior.
- **Base de datos**: H2 embebida en memoria para desarrollo (no requiere instalación), PostgreSQL real en producción (perfil `prod`).

---

## 🧩 Requisitos

- **Java JDK 17** o superior
- **Maven 3.8+** (o usar el wrapper del IDE)

Verifica tu versión de Java:
```bash
java -version
```

---

## 🚀 Cómo ejecutar en desarrollo

### Opción A — Desde la línea de comandos (Maven)
```bash
mvn spring-boot:run
```
Luego abre en el navegador: **http://localhost:8080**

### Opción B — Empaquetar y ejecutar el JAR
```bash
mvn clean package
java -jar target/imfundo-kahle.jar
```

Sin ninguna variable de entorno definida, la app arranca igual: usa valores por defecto pensados solo para desarrollo local (ver tabla de variables más abajo). El envío de correo y el reCAPTCHA simplemente no funcionarán hasta que definas esas variables.

### Consola H2 (solo desarrollo)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Disponible en **http://localhost:8080/h2-console** (usuario `sa`, contraseña vacía, JDBC URL `jdbc:h2:mem:imfundodb`). **Nunca actives este perfil en un servidor público.**

---

## 💻 Cómo abrir el proyecto en un IDE

### IntelliJ IDEA
1. `File` → `Open` → selecciona el `pom.xml` → `Open as Project`
2. Espera la sincronización de Maven
3. Ejecuta la clase `ImfundoKahleApplication` con ▶ Run

### Eclipse
1. `File` → `Import...` → `Maven` → `Existing Maven Projects`
2. Selecciona la carpeta del proyecto y espera a que Maven descargue las dependencias
3. Clic derecho en `ImfundoKahleApplication.java` → `Run As` → `Java Application`

### VS Code
Con la extensión "Extension Pack for Java" instalada, abre la carpeta del proyecto y ejecuta `ImfundoKahleApplication.java` con el botón "Run" que aparece sobre el método `main`.

---

## 🔑 Variables de entorno

Ninguna credencial ni clave queda escrita en el código: todo se lee de variables de entorno, con valores por defecto solo para desarrollo local (nunca usar esos valores por defecto en un servidor real).

| Variable | Para qué sirve | Obligatoria en producción |
|---|---|---|
| `ADMIN_EMAIL` | Correo de la cuenta administradora inicial | Recomendada |
| `ADMIN_PASSWORD` | Contraseña de esa cuenta administradora | **Sí** |
| `SEED_PASSWORD` | Contraseña compartida de las cuentas de demostración | Solo si `SEED_DEMO_USERS=true` |
| `SEED_DEMO_USERS` | `true`/`false`: crea o no ~68 cuentas de ejemplo al arrancar | No (ya es `false` en el perfil `prod`) |
| `MAIL_USERNAME` | Tu dirección de Gmail completa, para enviar los correos de recuperación de contraseña | **Sí** (si no, "olvidé mi contraseña" no funciona) |
| `MAIL_APP_PASSWORD` | "Contraseña de aplicación" de Gmail (no la contraseña normal de la cuenta) — se genera en https://myaccount.google.com/apppasswords, requiere verificación en 2 pasos activada | **Sí** |
| `MAIL_FROM` | Remitente que ve el usuario en el correo (opcional; si no se define, usa `MAIL_USERNAME`) | No |
| `RECAPTCHA_SITE_KEY` | Clave pública de Google reCAPTCHA v2 ("No soy un robot"), se pinta en el HTML | **Sí** |
| `RECAPTCHA_SECRET_KEY` | Clave secreta de reCAPTCHA, solo la usa el backend para verificar contra la API de Google | **Sí** |
| `PORT` | Puerto del servidor (el host lo define solo) | No |
| `SPRING_PROFILES_ACTIVE` | Poner en `prod` para usar PostgreSQL en vez de H2 en memoria | **Sí**, en producción |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Conexión a la base PostgreSQL real | **Sí**, en producción |

> ⚠️ **Las claves de reCAPTCHA son por dominio.** Si las que tienes fueron creadas para probar en `localhost`, agrega tu dominio real de producción en la [consola de Google reCAPTCHA](https://www.google.com/recaptcha/admin) antes de publicar el sitio — si no, el login, registro y recuperación de contraseña van a rechazar el captcha en producción.

---

## ☁️ Desplegar gratis para una prueba real (2026)

Railway y Fly.io ya no tienen plan gratuito real (piden tarjeta desde el primer día). Para probar esta app sin pagar nada, la combinación que sí funciona hoy es **Render** (hospeda la app) + **Neon** (base de datos PostgreSQL gratis permanente) — ninguno de los dos pide tarjeta de crédito.

> ⚠️ Cosas a tener en cuenta del plan gratis de Render (no son fallas del código, son límites del hosting gratuito):
> - El servicio "se duerme" tras 15 minutos sin visitas, y la primera visita después de eso tarda ~1 minuto en responder mientras despierta. No es que "se cayó": es normal en el plan gratis.
> - Si en vez de Neon usas la base de datos gratis de Render, esa expira a los 30 días. Por eso se recomienda Neon para la base de datos: su plan gratis no tiene fecha de vencimiento.

### Pasos

1. Crea una cuenta gratis en [neon.tech](https://neon.tech) y un proyecto Postgres nuevo. Copia la cadena de conexión que te da (formato `postgresql://usuario:contrasena@host/base?sslmode=require`).
2. Crea una cuenta gratis en [render.com](https://render.com), conecta este repositorio y crea un **Web Service** (Render detecta Maven solo gracias al `Procfile` incluido: `web: java -jar target/imfundo-kahle.jar`). Elige el plan **Free**.
3. En las variables de entorno del servicio en Render, define:
   ```
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:postgresql://<host-de-neon>/<tu-base>?sslmode=require
   SPRING_DATASOURCE_USERNAME=<usuario-de-neon>
   SPRING_DATASOURCE_PASSWORD=<contrasena-de-neon>
   ```
   (los datos exactos los saca de la cadena de conexión que copiaste de Neon en el paso 1).
4. Define el resto de las variables de la tabla de arriba (`ADMIN_PASSWORD`, `MAIL_USERNAME`, `MAIL_APP_PASSWORD`, `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, etc.) con tus valores reales.
5. Una vez que Render te asigne el dominio (`algo.onrender.com`), regístralo en la consola de Google reCAPTCHA (ver aviso arriba) — si no, el captcha va a rechazar todo en ese dominio.
6. Antes de dar por cerrada la prueba, verifica una vez completa: registro, login, "olvidé mi contraseña" (que llegue el correo de verdad), y el editor visual del contenido.

El perfil `prod` (`application-prod.properties`) ya usa `ddl-auto=update` (nunca borra datos reales al reiniciar, a diferencia del perfil de desarrollo) y ya trae `SEED_DEMO_USERS=false`: la base arranca vacía (solo la cuenta admin), igual que quedaría en un lanzamiento real — no hace falta "limpiarla" a mano.

> 📌 **Pendiente a mediano plazo**: el esquema de base de datos hoy lo infiere Hibernate automáticamente (`ddl-auto=update`). Funciona bien para esta prueba, pero si el proyecto sigue creciendo, lo ideal es migrar a **Flyway** o **Liquibase** para controlar los cambios de esquema de forma explícita.

> Si más adelante prefieres pagar por algo más estable/rápido que el plan gratis (sin el "dormido" de 15 minutos), Railway sigue siendo una opción sólida desde ~US$5/mes.

---

## 🔒 Seguridad

Este proyecto aplica varias capas de defensa, pensadas específicamente para que **no baste con escribir una URL para entrar donde no se debe**:

- **Consola H2 apagada por defecto.** Una consola SQL sin autenticación propia permitía ejecutar `UPDATE users SET role='ADMIN'...` y auto-otorgarse el rol de administrador sin pasar por ningún login. Solo se activa a propósito con el perfil `dev`, nunca en un servidor público.
- **Google reCAPTCHA v2** en login, registro y recuperación de contraseña, verificado siempre en el servidor contra la API de Google (nunca se confía en lo que manda el navegador).
- **Recuperación de contraseña sin filtración de información**: la respuesta es idéntica exista o no una cuenta con ese correo, y el código de 6 dígitos viaja en un token firmado (HMAC-SHA256) con 15 minutos de vigencia, sin guardarse en la base de datos.
- **Auto-registro controlado**: solo `@gmail.com`, solo rol Alumno o Profesor. Los roles Practicante y Administrador solo los asigna un administrador ya autenticado.
- **Bloqueo por fuerza bruta**: 3 intentos fallidos bloquean la cuenta 15 minutos.
- **CSRF activo en toda la aplicación**, incluidos el editor visual y la API de actividades.
- **Cabeceras de seguridad**: CSP a medida, HSTS, `X-Content-Type-Options: nosniff` en descargas e imágenes, cookie de sesión `HttpOnly` + `Secure` + `SameSite=Lax`.
- **Archivos e imágenes subidas** (materiales, contenido del sitio) se validan por sus primeros bytes reales, nunca por la extensión del nombre ni el tipo que declara el navegador.
- **Un usuario autenticado que visita una sección de otro rol** es redirigido a su propio panel en vez de ver un error técnico que revele la estructura del sitio.
- **Máximo una sesión activa por cuenta**: iniciar sesión en un dispositivo nuevo cierra la sesión anterior.
- **Cuentas deshabilitables** desde `/admin/users`, sin necesidad de borrar el usuario.

---

## 🧱 Estructura del proyecto

```
infundo/
├── pom.xml
├── Procfile
├── README.md
└── src/main/
    ├── java/com/imfundokahle/
    │   ├── ImfundoKahleApplication.java   # Clase principal
    │   ├── config/                        # Seguridad, i18n, datos iniciales, seeders
    │   ├── controller/                    # Controladores MVC (portales, contenido, auth)
    │   ├── dto/                           # Formularios (registro, login, recuperar contraseña)
    │   ├── model/                         # Entidades JPA y enums
    │   ├── repository/                    # Repositorios Spring Data JPA
    │   ├── security/                      # Filtro de reCAPTCHA
    │   ├── service/                       # Lógica de negocio (correo, reCAPTCHA, contenido editable...)
    │   └── validation/                    # Validaciones a medida (p. ej. @GmailAddress)
    └── resources/
        ├── application.properties          # Configuración por defecto (dev local)
        ├── application-dev.properties      # Perfil dev: consola H2, cookies sin HTTPS
        ├── application-prod.properties     # Perfil prod: PostgreSQL, sin usuarios demo
        ├── META-INF/additional-spring-configuration-metadata.json
        ├── messages*.properties            # Textos ES / FR / EN
        ├── static/css/style.css            # Estilos azul marino / naranja
        ├── static/js/hero3d.js             # Escena 3D decorativa del hero
        └── templates/                      # Vistas Thymeleaf
            ├── layout/fragments.html       # Marca, navbars, sidebars, mensajes flash
            ├── admin/contenido.html        # Panel clásico de contenido del sitio
            └── index.html                  # Landing page pública + editor visual en línea
```

> El paquete Java (`com.imfundokahle`) y el nombre de la base de datos (`imfundodb`) conservan su nombre técnico original; renombrarlos no aporta valor al usuario final y arriesga romper referencias internas, por lo que el rebranding se aplicó a todo lo que la persona usuaria realmente ve: interfaz, textos, metadatos de la app y documentación.

---

## 🛠️ Tecnologías

- Spring Boot 3.3 · Spring MVC · Spring Security · Spring Data JPA
- Thymeleaf + Thymeleaf Security Extras
- H2 (desarrollo) / PostgreSQL (producción)
- Google reCAPTCHA v2 · Spring Mail (Gmail SMTP)
- HTML5 Canvas + JavaScript (efectos del hero, editor visual, contadores animados)
- CSS3 personalizado (sin frameworks pesados), tipografías Sora + Inter vía Google Fonts

---

## 📝 Notas

- El puerto por defecto es **8080** (`server.port`, o la variable `PORT` en producción).
- Todo el texto de la interfaz está internacionalizado; usa el selector **ES / FR / EN** de la barra superior.
- Los precios mostrados en la sección "Precios" del landing son de ejemplo (editables desde `/admin/contenido` o el editor visual); ajústalos a tus tarifas reales antes de publicar el sitio.
- El correo de contacto del pie de página (`hola@vivaidiomas.com`) y los datos de ubicación son de ejemplo; actualízalos con los datos reales del centro antes de publicar.

---

© 2026 Viva Idiomas — Empoderando a mujeres y niños a través de la educación en idiomas.
