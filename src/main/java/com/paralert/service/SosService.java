package com.paralert.service;

import com.paralert.dto.request.SosRequest;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.SosResponse;
import com.paralert.entity.*;
import com.paralert.entity.enums.EstadoAlerta;
import com.paralert.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SosService {

    private final SosAlertaRepository sosAlertaRepository;
    private final AlertaEvidenciaRepository alertaEvidenciaRepository;
    private final AlertaEnviadaRepository alertaEnviadaRepository;
    private final ContactoRepository contactoRepository;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final ZonaPeligrosaRepository zonaPeligrosaRepository;

    // =====================================================
    // DISPARAR SOS
    // =====================================================
    @Transactional
    public SosResponse dispararSos(Usuario usuario, SosRequest request) {
        // Enforce 10-minute cooldown for SOS alerts
        List<SosAlerta> alertasPrevias = sosAlertaRepository.findByUsuarioOrderByFechaCreacionDesc(usuario);
        if (!alertasPrevias.isEmpty()) {
            SosAlerta ultima = alertasPrevias.get(0);
            if (ultima.getFechaCreacion().plusMinutes(10).isAfter(LocalDateTime.now())) {
                long segundosRestantes = java.time.Duration.between(LocalDateTime.now(), ultima.getFechaCreacion().plusMinutes(10)).toSeconds();
                long minutos = segundosRestantes / 60;
                long segundos = segundosRestantes % 60;
                throw new IllegalStateException("No puedes volver a reportar un SOS dentro de los 10 minutos de tu última alerta. Intenta de nuevo en " + minutos + "m " + segundos + "s.");
            }
        }

        SosAlerta alerta = SosAlerta.builder()
                .usuario(usuario)
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .mensaje(request.getMensaje())
                .estado(EstadoAlerta.ACTIVA)
                .build();

        sosAlertaRepository.save(alerta);

        // Buscar zona a menos de 130m y sumarle +20 puntos
        double reqLat = request.getLatitud().doubleValue();
        double reqLon = request.getLongitud().doubleValue();
        List<ZonaPeligrosa> zonas = zonaPeligrosaRepository.findAll();
        double menorDist = Double.MAX_VALUE;
        ZonaPeligrosa zonaCercana = null;

        for (ZonaPeligrosa z : zonas) {
            if ("CERRADA".equalsIgnoreCase(z.getEstado())) {
                continue;
            }
            double dist = calcularDistanciaHaversine(reqLat, reqLon, z.getLatitud().doubleValue(), z.getLongitud().doubleValue());
            if (dist <= 130.0 && dist < menorDist) {
                menorDist = dist;
                zonaCercana = z;
            }
        }

        if (zonaCercana != null) {
            int nuevosPuntos = (zonaCercana.getPuntaje() != null ? zonaCercana.getPuntaje() : 10) + 20;
            zonaCercana.setPuntaje(nuevosPuntos);
            zonaCercana.setFechaUltimaActividad(LocalDateTime.now());
            zonaCercana.setRadio(ZonaPeligrosaService.calcularRadio(nuevosPuntos));
            zonaCercana.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(nuevosPuntos));
            if ("OBSERVACION".equals(zonaCercana.getEstado()) && nuevosPuntos > 30) {
                zonaCercana.setEstado("ACTIVA");
            }
            zonaPeligrosaRepository.save(zonaCercana);
            log.info("Alerta SOS disparada en radio de zona #{}. +20 puntos, total: {}", zonaCercana.getId(), nuevosPuntos);
        }

        // Notificar a todos los contactos de confianza
        List<Contacto> contactos = contactoRepository.findByUsuario(usuario);
        int notificados = 0;

        for (Contacto c : contactos) {
            try {
                emailService.enviarAlertaSos(
                        c.getContacto().getEmail(),
                        usuario.getNombres() + " " + (usuario.getApellidos() != null ? usuario.getApellidos() : ""),
                        request.getLatitud(),
                        request.getLongitud(),
                        request.getMensaje()
                );

                AlertaEnviada enviada = AlertaEnviada.builder()
                        .alerta(alerta)
                        .contacto(c.getContacto())
                        .email(c.getContacto().getEmail())
                        .enviado(true)
                        .build();

                alertaEnviadaRepository.save(enviada);
                notificados++;
            } catch (Exception e) {
                log.error("Error notificando a contacto {}: {}", c.getContacto().getEmail(), e.getMessage());

                AlertaEnviada fallida = AlertaEnviada.builder()
                        .alerta(alerta)
                        .contacto(c.getContacto())
                        .email(c.getContacto().getEmail())
                        .enviado(false)
                        .build();

                alertaEnviadaRepository.save(fallida);
            }
        }

        // Enviar correo de confirmación al propio usuario
        try {
            emailService.enviarConfirmacionSosUsuario(
                    usuario.getEmail(),
                    usuario.getNombres() + " " + (usuario.getApellidos() != null ? usuario.getApellidos() : ""),
                    request.getLatitud(),
                    request.getLongitud(),
                    request.getMensaje()
            );
        } catch (Exception e) {
            log.error("Error enviando correo de confirmación SOS al usuario: {}", e.getMessage());
        }

        log.info("SOS disparado por usuario {} - Alerta #{} - {} contactos notificados",
                usuario.getEmail(), alerta.getId(), notificados);

        return mapearAlerta(alerta, notificados);
    }

    // =====================================================
    // SUBIR EVIDENCIA (foto) a una alerta
    // =====================================================
    @Transactional
    public MessageResponse subirEvidencia(Usuario usuario, Long alertaId, MultipartFile archivo)
            throws IOException {

        SosAlerta alerta = sosAlertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        if (!alerta.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("No puedes subir evidencia a esta alerta");
        }

        if (alerta.getEstado() != EstadoAlerta.ACTIVA) {
            throw new IllegalStateException("Solo puedes subir evidencia a alertas activas");
        }

        String url = cloudinaryService.subirEvidenciaSos(archivo, alertaId);

        AlertaEvidencia evidencia = AlertaEvidencia.builder()
                .alerta(alerta)
                .urlImagen(url)
                .build();

        alertaEvidenciaRepository.save(evidencia);

        return new MessageResponse("Evidencia subida correctamente");
    }

    // =====================================================
    // CERRAR / CANCELAR ALERTA
    // =====================================================
    @Transactional
    public SosResponse cambiarEstadoAlerta(Usuario usuario, Long alertaId, String nuevoEstado) {
        SosAlerta alerta = sosAlertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        if (!alerta.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("No puedes modificar esta alerta");
        }

        if (alerta.getEstado() != EstadoAlerta.ACTIVA) {
            throw new IllegalStateException("La alerta ya está cerrada");
        }

        EstadoAlerta estado;
        try {
            estado = EstadoAlerta.valueOf(nuevoEstado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido. Usa: ATENDIDA o CANCELADA");
        }

        if (estado == EstadoAlerta.ACTIVA) {
            throw new IllegalArgumentException("No puedes cambiar a estado ACTIVA");
        }

        alerta.setEstado(estado);
        alerta.setFechaCierre(LocalDateTime.now());
        sosAlertaRepository.save(alerta);

        return mapearAlerta(alerta, null);
    }

    // =====================================================
    // HISTORIAL DE ALERTAS
    // =====================================================
    public List<SosResponse> obtenerHistorial(Usuario usuario) {
        return sosAlertaRepository.findByUsuarioOrderByFechaCreacionDesc(usuario)
                .stream()
                .map(a -> mapearAlerta(a, null))
                .collect(Collectors.toList());
    }

    // =====================================================
    // VER ALERTA DETALLE
    // =====================================================
    public SosResponse obtenerAlerta(Usuario usuario, Long alertaId) {
        SosAlerta alerta = sosAlertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada"));

        if (!alerta.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("No tienes acceso a esta alerta");
        }

        return mapearAlerta(alerta, null);
    }

    // =====================================================
    // MAPEO
    // =====================================================
    private SosResponse mapearAlerta(SosAlerta alerta, Integer contactosNotificados) {
        List<String> evidencias = alertaEvidenciaRepository.findByAlerta(alerta)
                .stream()
                .map(AlertaEvidencia::getUrlImagen)
                .collect(Collectors.toList());

        int notificados = contactosNotificados != null
                ? contactosNotificados
                : alertaEnviadaRepository.findByAlerta(alerta).size();

        return SosResponse.builder()
                .id(alerta.getId())
                .latitud(alerta.getLatitud())
                .longitud(alerta.getLongitud())
                .mensaje(alerta.getMensaje())
                .estado(alerta.getEstado().name())
                .fechaCreacion(alerta.getFechaCreacion())
                .fechaCierre(alerta.getFechaCierre())
                .evidencias(evidencias)
                .contactosNotificados(notificados)
                .build();
    }

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
