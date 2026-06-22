package com.paralert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final RestClient restClient;

    @Value("${brevo.sender.name}")
    private String senderName;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    public EmailService(TemplateEngine templateEngine,
                        @Value("${brevo.api-key}") String apiKey) {
        this.templateEngine = templateEngine;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

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
        Map<String, Object> sender = Map.of(
                "name", senderName,
                "email", senderEmail
        );
        Map<String, Object> to = Map.of(
                "email", destinatario
        );
        Map<String, Object> payload = Map.of(
                "sender", sender,
                "to", List.of(to),
                "subject", asunto,
                "htmlContent", html
        );

        restClient.post()
                .uri("/smtp/email")
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("Correo enviado exitosamente a {} vía Brevo API", destinatario);
    }
}
