package com.banduairlines.service.impl;

import com.banduairlines.exception.ServicioExternoException;
import com.banduairlines.service.cuenta.ServicioCorreo;
import com.banduairlines.service.cuenta.ServicioCorreoRecuperacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ServicioCorreoSmtpImpl
        implements ServicioCorreo, ServicioCorreoRecuperacion {

    private final JavaMailSender mailSender;
    private final String remitente;

    public ServicioCorreoSmtpImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.remitente}") String remitente) {

        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviarCodigoCambioCorreo(
            String destinatario,
            String codigo,
            Instant fechaExpiracion) {

        String asunto = "BanduAirlines - Cambio de correo";

        String contenido = """
                Hola:

                Has solicitado cambiar la direccion de correo
                electronico asociada a tu cuenta BanduAirlines.

                Tu codigo de verificacion es:

                %s

                Este codigo expirara el: %s (UTC).

                Si no solicitaste este cambio,
                puedes ignorar este mensaje.

                Atentamente,
                Equipo BanduAirlines
                """.formatted(codigo, fechaExpiracion);

        enviarCorreo(destinatario, asunto, contenido);
    }

    @Override
    public void enviarEnlaceRecuperacion(
            String destinatario,
            String enlaceRecuperacion,
            Instant fechaExpiracion) {

        String asunto = "BanduAirlines - Recuperar contraseña";

        String contenido = """
                Hola:

                Hemos recibido una solicitud para recuperar
                la contraseña de tu cuenta BanduAirlines.

                Puedes restablecerla mediante este enlace:

                %s

                El enlace expirara el: %s (UTC).

                Por seguridad, el enlace solamente
                puede utilizarse una vez.

                Si no solicitaste esta recuperacion,
                puedes ignorar este mensaje.

                Atentamente,
                Equipo BanduAirlines
                """.formatted(
                enlaceRecuperacion,
                fechaExpiracion
        );

        enviarCorreo(destinatario, asunto, contenido);
    }

    private void enviarCorreo(
            String destinatario,
            String asunto,
            String contenido) {

        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(contenido);

        try {
            mailSender.send(mensaje);

        } catch (MailException ex) {
            throw new ServicioExternoException(
                    "No fue posible enviar el correo electronico",
                    ex
            );
        }
    }
}
