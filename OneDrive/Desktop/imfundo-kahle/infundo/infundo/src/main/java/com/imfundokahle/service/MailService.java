package com.imfundokahle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Envio de correo saliente (recuperacion de contrasena) via SMTP. */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public MailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /** Manda el codigo de recuperacion de contrasena a un usuario. Nunca lanza al llamador. */
    @org.springframework.scheduling.annotation.Async
    public void enviarCodigoRecuperacion(String destinatario, String code) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(from);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Codigo de recuperacion - Viva Idiomas");
        mensaje.setText(
                "Recibimos una solicitud para restablecer tu contrasena.\n\n"
                + "Tu codigo de verificacion es: " + code + "\n\n"
                + "Este codigo vence en 15 minutos. Si tu no pediste este cambio, ignora este correo."
        );
        try {
            mailSender.send(mensaje);
            
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de recuperacion a {}: {}", destinatario, e.getMessage());
            
        }
    }

    /** Recordatorio de que una clase empieza pronto. Nunca lanza al llamador. */
    @org.springframework.scheduling.annotation.Async
    public void enviarRecordatorioClase(String destinatario, String materia, String horaInicio, String meetLink) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(from);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Tu clase de \"" + materia + "\" empieza pronto - Viva Idiomas");
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Tu clase \"").append(materia).append("\" empieza a las ").append(horaInicio)
                .append(" (en unos 30 minutos).\n\n");
        if (meetLink != null && !meetLink.isBlank()) {
            cuerpo.append("Enlace de la videollamada: ").append(meetLink).append("\n\n");
        }
        cuerpo.append("- Viva Idiomas");
        mensaje.setText(cuerpo.toString());
        try {
            mailSender.send(mensaje);
            
        } catch (Exception e) {
            log.error("No se pudo enviar el recordatorio de clase a {}: {}", destinatario, e.getMessage());
            
        }
    }
}
