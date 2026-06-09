package com.paralert.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void enviarCodigoVerificacion(String destinatario, String nombres, String codigo) {
        try {
            Context ctx = new Context();
            ctx.setVariable("nombres", nombres);
            ctx.setVariable("email", destinatario);
            ctx.setVariable("codigo", codigo);

            String html = templateEngine.process("emails/codigo-verificacion", ctx);
            enviarHtml(destinatario, "🔐 Tu código de verificación - Paralert", html);
        } catch (Exception e) {
            log.error("Error enviando código de verificación a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarRecuperarPassword(String destinatario, String nombres, String codigo) {
        try {
            Context ctx = new Context();
            ctx.setVariable("nombres", nombres);
            ctx.setVariable("codigo", codigo);

            String html = templateEngine.process("emails/recuperar-password", ctx);
            enviarHtml(destinatario, "🔑 Recuperar contraseña - Paralert", html);
        } catch (Exception e) {
            log.error("Error enviando recuperar password a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarSolicitudContacto(String destinatario, String nombresSolicitante,
                                        String usernameSolicitante) {
        try {
            Context ctx = new Context();
            ctx.setVariable("nombresSolicitante", nombresSolicitante);
            ctx.setVariable("usernameSolicitante", usernameSolicitante);

            String html = templateEngine.process("emails/solicitud-contacto", ctx);
            enviarHtml(destinatario, "👋 " + nombresSolicitante + " quiere agregarte - Paralert", html);
        } catch (Exception e) {
            log.error("Error enviando solicitud contacto a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarAlertaSos(String destinatario, String nombresVictima,
                                BigDecimal latitud, BigDecimal longitud, String mensaje) {
        try {
            String urlMaps = "https://maps.google.com/?q=" + latitud + "," + longitud;

            Context ctx = new Context();
            ctx.setVariable("nombresVictima", nombresVictima);
            ctx.setVariable("latitud", latitud);
            ctx.setVariable("longitud", longitud);
            ctx.setVariable("urlMaps", urlMaps);
            ctx.setVariable("mensaje", mensaje);

            String html = templateEngine.process("emails/alerta-sos", ctx);
            enviarHtml(destinatario, "🚨 ALERTA SOS - " + nombresVictima + " necesita ayuda", html);
        } catch (Exception e) {
            log.error("Error enviando alerta SOS a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarConfirmacionSosUsuario(String destinatario, String nombresUsuario,
                                            BigDecimal latitud, BigDecimal longitud, String mensaje) {
        try {
            String urlMaps = "https://maps.google.com/?q=" + latitud + "," + longitud;

            Context ctx = new Context();
            ctx.setVariable("nombresUsuario", nombresUsuario);
            ctx.setVariable("latitud", latitud);
            ctx.setVariable("longitud", longitud);
            ctx.setVariable("urlMaps", urlMaps);
            ctx.setVariable("mensaje", mensaje);

            String html = templateEngine.process("emails/confirmacion-sos", ctx);
            enviarHtml(destinatario, "✅ Tu reporte SOS ha sido enviado - Paralert", html);
        } catch (Exception e) {
            log.error("Error enviando confirmacion SOS a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarAlertaProximidad(String destinatario, String nombresUsuario,
                                       BigDecimal latitud, BigDecimal longitud, String zonaNombre, String nivelRiesgo) {
        try {
            String urlMaps = "https://maps.google.com/?q=" + latitud + "," + longitud;

            Context ctx = new Context();
            ctx.setVariable("nombresUsuario", nombresUsuario);
            ctx.setVariable("latitud", latitud);
            ctx.setVariable("longitud", longitud);
            ctx.setVariable("urlMaps", urlMaps);
            ctx.setVariable("zonaNombre", zonaNombre);
            ctx.setVariable("nivelRiesgo", nivelRiesgo);
            ctx.setVariable("mensaje", "Se encuentra transitando cerca del sector de peligro '" + zonaNombre + "' que posee un riesgo calificado como " + nivelRiesgo.toUpperCase() + ". Mantente alerta.");

            String html = templateEngine.process("emails/alerta-proximidad", ctx);
            String emoji = "alto".equalsIgnoreCase(nivelRiesgo) ? "🚨" : "⚠️";
            enviarHtml(destinatario, emoji + " ALERTA DE PROXIMIDAD DE RIESGO " + nivelRiesgo.toUpperCase() + " - " + nombresUsuario, html);
        } catch (Exception e) {
            log.error("Error enviando correo de proximidad a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarAdvertenciaMalUso(String destinatario, String nombresUsuario, String motivo,
                                        int nivelConfianza, int cantidadAdvertencias) {
        try {
            Context ctx = new Context();
            ctx.setVariable("nombresUsuario", nombresUsuario);
            ctx.setVariable("motivo", motivo);
            ctx.setVariable("nivelConfianza", nivelConfianza);
            ctx.setVariable("cantidadAdvertencias", cantidadAdvertencias);

            String html = templateEngine.process("emails/alerta-advertencia", ctx);
            enviarHtml(destinatario, "⚠️ ADVERTENCIA: Mal uso de la plataforma Paralert", html);
        } catch (Exception e) {
            log.error("Error enviando correo de advertencia a {}: {}", destinatario, e.getMessage());
        }
    }

    @Async
    public void enviarBloqueoCuenta(String destinatario, String nombresUsuario, String motivo,
                                    int nivelConfianza, int cantidadAdvertencias) {
        try {
            Context ctx = new Context();
            ctx.setVariable("nombresUsuario", nombresUsuario);
            ctx.setVariable("motivo", motivo);
            ctx.setVariable("nivelConfianza", nivelConfianza);
            ctx.setVariable("cantidadAdvertencias", cantidadAdvertencias);

            String html = templateEngine.process("emails/alerta-bloqueo", ctx);
            enviarHtml(destinatario, "🚨 CUENTA BLOQUEADA PERMANENTEMENTE - Paralert", html);
        } catch (Exception e) {
            log.error("Error enviando correo de bloqueo a {}: {}", destinatario, e.getMessage());
        }
    }



    private void enviarHtml(String destinatario, String asunto, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(html, true);
        mailSender.send(message);
        log.info("Correo enviado a: {}", destinatario);
    }
}
