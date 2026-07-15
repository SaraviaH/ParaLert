package com.paralert.service;

import com.paralert.dto.request.ComentarioZonaRequest;
import com.paralert.dto.request.ZonaPeligrosaRequest;
import com.paralert.dto.response.ComentarioZonaResponse;
import com.paralert.dto.response.ZonaPeligrosaResponse;
import com.paralert.dto.response.ReporteResponse;
import com.paralert.entity.ComentarioZona;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import com.paralert.entity.Reporte;
import com.paralert.entity.Confirmacion;
import com.paralert.entity.Amigo;
import com.paralert.repository.ComentarioZonaRepository;
import com.paralert.repository.ZonaPeligrosaRepository;
import com.paralert.repository.TipoPeligroRepository;
import com.paralert.repository.AmigoRepository;
import com.paralert.repository.RegistroProximidadRepository;
import com.paralert.repository.ReporteRepository;
import com.paralert.repository.ConfirmacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paralert.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZonaPeligrosaService {

    private final ZonaPeligrosaRepository zonaPeligrosaRepository;
    private final ComentarioZonaRepository comentarioZonaRepository;
    private final CloudinaryService cloudinaryService;
    private final TipoPeligroRepository tipoPeligroRepository;
    private final AmigoRepository amigoRepository;
    private final EmailService emailService;
    private final RegistroProximidadRepository registroProximidadRepository;
    private final ReporteRepository reporteRepository;
    private final ConfirmacionRepository confirmacionRepository;
    private final IaAnalysisService iaAnalysisService;


    // =====================================================
    // LISTAR TODAS LAS ZONAS DE RIESGO
    // =====================================================
    @Transactional(readOnly = true)
    public List<ZonaPeligrosaResponse> obtenerTodasLasZonas() {
        return obtenerTodasLasZonas(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ZonaPeligrosaResponse> obtenerTodasLasZonas(
            java.math.BigDecimal minLat, java.math.BigDecimal maxLat,
            java.math.BigDecimal minLon, java.math.BigDecimal maxLon) {
        
        List<ZonaPeligrosa> zonas;
        if (minLat != null && maxLat != null && minLon != null && maxLon != null) {
            zonas = zonaPeligrosaRepository.findZonesInViewport(minLat, maxLat, minLon, maxLon);
        } else {
            zonas = zonaPeligrosaRepository.findAllWithUsuarioAndTipoPeligro();
        }

        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        Set<Long> zonasConfirmadasIds = new HashSet<>();
        if (usuarioAutenticado != null) {
            zonasConfirmadasIds = confirmacionRepository.findZonaIdsConfirmadasPorUsuario(usuarioAutenticado.getId());
        }

        final Set<Long> finalZonasConfirmadasIds = zonasConfirmadasIds;
        return zonas.stream()
                .map(z -> mapearZonaResponse(z, finalZonasConfirmadasIds))
                .collect(Collectors.toList());
    }

    // =====================================================
    // REGISTRAR NUEVA ZONA
    // =====================================================
    @Transactional
    public ZonaPeligrosaResponse crearZona(Usuario usuario, ZonaPeligrosaRequest request) {
        com.paralert.entity.TipoPeligro tipo = null;
        if (request.getTipoPeligroId() != null) {
            tipo = tipoPeligroRepository.findById(request.getTipoPeligroId()).orElse(null);
        }

        String tituloFinal = (tipo != null) ? tipo.getNombre() : (request.getTitulo() != null ? request.getTitulo() : "Incidente");

        ZonaPeligrosa zona = null;
        if (request.getAsociarZonaId() != null) {
            zona = zonaPeligrosaRepository.findById(request.getAsociarZonaId()).orElse(null);
        }

        // Si no se especifica asociarZonaId ni forzarCreacion, buscamos zonas activas en un radio de 130m
        if (zona == null && !Boolean.TRUE.equals(request.getForzarCreacion())) {
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
                // Hay una zona cercana, reportamos el conflicto al frontend con código HTTP 409
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "ZONA_EXISTENTE:" + zonaCercana.getId());
            }
        }

        boolean esNuevaZona = (zona == null);
        if (zona == null) {
            // Crear nueva zona peligrosa en observación
            zona = ZonaPeligrosa.builder()
                    .usuario(usuario)
                    .titulo(tituloFinal)
                    .tipoPeligro(tipo)
                    .descripcion(request.getDescripcion())
                    .latitud(request.getLatitud())
                    .longitud(request.getLongitud())
                    .nivelRiesgo("OBSERVACION")
                    .puntaje(10)
                    .radio(10)
                    .estado("OBSERVACION")
                    .fechaUltimaActividad(LocalDateTime.now())
                    .build();
            zona = zonaPeligrosaRepository.save(zona);
            log.info("Nueva zona registrada en observación: #{} - {}", zona.getId(), zona.getTitulo());
        } else {
            // Añadir reporte a zona existente: +10 puntos
            int nuevosPuntos = (zona.getPuntaje() != null ? zona.getPuntaje() : 10) + 10;
            zona.setPuntaje(nuevosPuntos);
            zona.setFechaUltimaActividad(LocalDateTime.now());
            
            // Recalcular radio y nivel de riesgo
            zona.setRadio(calcularRadio(nuevosPuntos));
            zona.setNivelRiesgo(calcularNivelRiesgo(nuevosPuntos));

            if ("OBSERVACION".equals(zona.getEstado()) && nuevosPuntos > 30) {
                zona.setEstado("ACTIVA");
            }
            
            // Corregido: guardar después de aplicar puntos
            zona = zonaPeligrosaRepository.save(zona);
            log.info("Reporte añadido a zona existente #{}. Nuevos puntos: {}, radio: {}", zona.getId(), nuevosPuntos, zona.getRadio());
        }

        // Crear registro de reporte
        Reporte reporte = Reporte.builder()
                .usuario(usuario)
                .zona(zona)
                .tipoPeligro(tipo)
                .descripcion(request.getDescripcion())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .build();
        reporteRepository.save(reporte);

        // Moderar reporte automáticamente de forma local
        iaAnalysisService.analizarReporte(reporte);

        if (Boolean.TRUE.equals(reporte.getSospechoso())) {
            log.warn("Reporte marcado como sospechoso de inmediato. Reajustando puntos de la zona #{}", zona.getId());
            if (esNuevaZona) {
                zona.setPuntaje(0);
                zona.setRadio(calcularRadio(0));
                zona.setNivelRiesgo("OBSERVACION");
                zonaPeligrosaRepository.save(zona);
            } else {
                int nuevosPuntos = Math.max(0, zona.getPuntaje() - 10);
                zona.setPuntaje(nuevosPuntos);
                zona.setRadio(calcularRadio(nuevosPuntos));
                zona.setNivelRiesgo(calcularNivelRiesgo(nuevosPuntos));
                if (nuevosPuntos <= 30) {
                    zona.setEstado("OBSERVACION");
                }
                zonaPeligrosaRepository.save(zona);
            }
        }

        return mapearZonaResponse(zona);
    }

    // =====================================================
    // LISTAR COMENTARIOS DE UNA ZONA
    // =====================================================
    @Transactional(readOnly = true)
    public List<ComentarioZonaResponse> obtenerComentariosDeZona(Long zonaId) {
        ZonaPeligrosa zona = zonaPeligrosaRepository.findById(zonaId)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));

        return comentarioZonaRepository.findByZonaWithUsuarioOrderByFechaCreacionAsc(zona)
                .stream()
                .map(this::mapearComentarioResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // AGREGAR COMENTARIO Y ESTRELLAS A UNA ZONA
    // =====================================================
    @Transactional
    public ComentarioZonaResponse agregarComentario(Usuario usuario, Long zonaId, ComentarioZonaRequest request) {
        ZonaPeligrosa zona = zonaPeligrosaRepository.findById(zonaId)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));

        ComentarioZona comentario = ComentarioZona.builder()
                .zona(zona)
                .usuario(usuario)
                .contenido(request.getContenido())
                .calificacion(request.getCalificacion())
                .build();

        ComentarioZona guardado = comentarioZonaRepository.save(comentario);
        
        // Moderar comentario automáticamente de forma local
        iaAnalysisService.analizarComentario(guardado);

        if (Boolean.TRUE.equals(guardado.getSospechoso())) {
            log.warn("Comentario marcado como sospechoso de inmediato. No se añaden puntos a la zona #{}", zona.getId());
        } else {
            // Sumar +2 puntos a la zona por el comentario
            int nuevosPuntos = (zona.getPuntaje() != null ? zona.getPuntaje() : 10) + 2;
            zona.setPuntaje(nuevosPuntos);
            zona.setFechaUltimaActividad(LocalDateTime.now());
            zona.setRadio(calcularRadio(nuevosPuntos));
            zona.setNivelRiesgo(calcularNivelRiesgo(nuevosPuntos));
            if ("OBSERVACION".equals(zona.getEstado()) && nuevosPuntos > 30) {
                zona.setEstado("ACTIVA");
            }
            zonaPeligrosaRepository.save(zona);
            log.info("Nuevo comentario añadido a zona #{} por usuario {}. +2 puntos, total: {}", zonaId, usuario.getEmail(), nuevosPuntos);
        }

        return mapearComentarioResponse(guardado);
    }

    @Transactional
    public ZonaPeligrosaResponse subirFotoZona(Usuario usuario, Long zonaId, org.springframework.web.multipart.MultipartFile archivo) throws java.io.IOException {
        ZonaPeligrosa zona = zonaPeligrosaRepository.findById(zonaId)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));

        String url = cloudinaryService.subirImagenZona(archivo, zonaId);
        zona.setFotoUrl(url);

        // Sumar +8 puntos por evidencia fotográfica
        int nuevosPuntos = (zona.getPuntaje() != null ? zona.getPuntaje() : 10) + 8;
        zona.setPuntaje(nuevosPuntos);
        zona.setFechaUltimaActividad(LocalDateTime.now());
        zona.setRadio(calcularRadio(nuevosPuntos));
        zona.setNivelRiesgo(calcularNivelRiesgo(nuevosPuntos));
        if ("OBSERVACION".equals(zona.getEstado()) && nuevosPuntos > 30) {
            zona.setEstado("ACTIVA");
        }
        zonaPeligrosaRepository.save(zona);

        // Asociar la foto al último reporte de este usuario en esta zona
        List<Reporte> reportesUsuario = reporteRepository.findByZona(zona).stream()
                .filter(r -> r.getUsuario().getId().equals(usuario.getId()))
                .sorted((r1, r2) -> r2.getFechaCreacion().compareTo(r1.getFechaCreacion()))
                .collect(Collectors.toList());
        if (!reportesUsuario.isEmpty()) {
            Reporte r = reportesUsuario.get(0);
            r.setFotoUrl(url);
            reporteRepository.save(r);
        }

        log.info("Foto de evidencia subida para zona #{} por usuario {}. +8 puntos, total: {}", zonaId, usuario.getEmail(), nuevosPuntos);
        return mapearZonaResponse(zona);
    }

    @Transactional
    public ComentarioZonaResponse subirFotoComentario(Usuario usuario, Long comentarioId, org.springframework.web.multipart.MultipartFile archivo) throws java.io.IOException {
        ComentarioZona comentario = comentarioZonaRepository.findById(comentarioId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));

        String url = cloudinaryService.subirImagenComentario(archivo, comentarioId);
        comentario.setFotoUrl(url);
        comentarioZonaRepository.save(comentario);

        // Sumar +8 puntos por evidencia fotográfica
        ZonaPeligrosa zona = comentario.getZona();
        int nuevosPuntos = (zona.getPuntaje() != null ? zona.getPuntaje() : 10) + 8;
        zona.setPuntaje(nuevosPuntos);
        zona.setFechaUltimaActividad(LocalDateTime.now());
        zona.setRadio(calcularRadio(nuevosPuntos));
        zona.setNivelRiesgo(calcularNivelRiesgo(nuevosPuntos));
        if ("OBSERVACION".equals(zona.getEstado()) && nuevosPuntos > 30) {
            zona.setEstado("ACTIVA");
        }
        zonaPeligrosaRepository.save(zona);

        log.info("Foto de evidencia subida para comentario #{} en zona #{}. +8 puntos, total: {}", comentarioId, zona.getId(), nuevosPuntos);
        return mapearComentarioResponse(comentario);
    }

    @Transactional
    public ZonaPeligrosaResponse confirmarZona(Usuario usuario, Long id) {
        // Bloqueo pesimista de escritura para evitar condiciones de carrera
        ZonaPeligrosa zona = zonaPeligrosaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));

        java.util.Optional<Confirmacion> existingOpt = confirmacionRepository.findByUsuarioAndZona(usuario, zona);
        
        int nuevosPuntos = zona.getPuntaje() != null ? zona.getPuntaje() : 10;
        if (existingOpt.isPresent()) {
            // Toggle off: eliminar confirmación y decrementar puntaje
            Confirmacion conf = existingOpt.get();
            confirmacionRepository.delete(conf);
            if (zona.getConfirmaciones() != null) {
                zona.getConfirmaciones().removeIf(c -> c.getId() != null && c.getId().equals(conf.getId()));
            }
            nuevosPuntos -= 5;
            log.info("Zona #{} confirmación removida por usuario {}. -5 puntos, total: {}", id, usuario.getEmail(), nuevosPuntos);
        } else {
            // Toggle on: crear confirmación e incrementar puntaje
            Confirmacion confirmacion = Confirmacion.builder()
                    .usuario(usuario)
                    .zona(zona)
                    .build();
            confirmacion = confirmacionRepository.save(confirmacion);
            if (zona.getConfirmaciones() != null) {
                zona.getConfirmaciones().add(confirmacion);
            }
            nuevosPuntos += 5;
            log.info("Zona #{} confirmada por usuario {}. +5 puntos, total: {}", id, usuario.getEmail(), nuevosPuntos);
        }

        if (nuevosPuntos < 0) {
            nuevosPuntos = 0;
        }

        zona.setPuntaje(nuevosPuntos);
        zona.setFechaUltimaActividad(LocalDateTime.now());
        zona.setRadio(calcularRadio(nuevosPuntos));
        zona.setNivelRiesgo(calcularNivelRiesgo(nuevosPuntos));
        
        if ("OBSERVACION".equals(zona.getNivelRiesgo())) {
            zona.setEstado("OBSERVACION");
        } else {
            zona.setEstado("ACTIVA");
        }
        
        zonaPeligrosaRepository.save(zona);
        return mapearZonaResponse(zona);
    }

    // =====================================================
    // ELIMINAR ZONA (MODERACIÓN ADMIN)
    // =====================================================
    @Transactional
    public void eliminarZona(Long id) {
        ZonaPeligrosa zona = zonaPeligrosaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));

        zonaPeligrosaRepository.delete(zona);
        log.info("Zona peligrosa #{} eliminada mediante moderación", id);
    }

    // =====================================================
    // MAPEO A RESPUESTAS
    // =====================================================
    private Usuario obtenerUsuarioAutenticado() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUsuario();
        }
        return null;
    }

    private ZonaPeligrosaResponse mapearZonaResponse(ZonaPeligrosa zona) {
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        Set<Long> zonasConfirmadasIds = new HashSet<>();
        if (usuarioAutenticado != null) {
            if (confirmacionRepository.existsByUsuarioAndZona(usuarioAutenticado, zona)) {
                zonasConfirmadasIds.add(zona.getId());
            }
        }
        return mapearZonaResponse(zona, zonasConfirmadasIds);
    }

    private ZonaPeligrosaResponse mapearZonaResponse(ZonaPeligrosa zona, Set<Long> zonasConfirmadasIds) {
        List<ComentarioZona> comentarios = zona.getComentarios();
        
        double calificacionPromedio = 0.0;
        if (comentarios != null && !comentarios.isEmpty()) {
            calificacionPromedio = comentarios.stream()
                    .mapToInt(ComentarioZona::getCalificacion)
                    .average()
                    .orElse(0.0);
        }

        int totalComentarios = comentarios != null ? comentarios.size() : 0;
        String creadorNombre = zona.getUsuario().getNombres() + " " + 
                (zona.getUsuario().getApellidos() != null ? zona.getUsuario().getApellidos() : "");

        List<ReporteResponse> reportesList = new java.util.ArrayList<>();
        if (zona.getReportes() != null) {
            reportesList = zona.getReportes().stream()
                    .map(this::mapearReporteResponse)
                    .collect(Collectors.toList());
        }

        boolean verificado = zonasConfirmadasIds.contains(zona.getId());

        return ZonaPeligrosaResponse.builder()
                .id(zona.getId())
                .titulo(zona.getTitulo())
                .descripcion(zona.getDescripcion())
                .latitud(zona.getLatitud())
                .longitud(zona.getLongitud())
                .nivelRiesgo(zona.getNivelRiesgo())
                .puntaje(zona.getPuntaje() != null ? zona.getPuntaje() : 10)
                .radio(zona.getRadio() != null ? zona.getRadio() : 10)
                .estado(zona.getEstado() != null ? zona.getEstado() : "OBSERVACION")
                .fechaCreacion(zona.getFechaCreacion())
                .fechaUltimaActividad(zona.getFechaUltimaActividad() != null ? zona.getFechaUltimaActividad() : zona.getFechaCreacion())
                .creadorNombre(creadorNombre)
                .calificacionPromedio(calificacionPromedio)
                .totalComentarios(totalComentarios)
                .totalConfirmaciones(zona.getConfirmaciones() != null ? zona.getConfirmaciones().size() : 0)
                .fotoUrl(zona.getFotoUrl())
                .tipoPeligroId(zona.getTipoPeligro() != null ? zona.getTipoPeligro().getId() : null)
                .creadorNivelConfianza(zona.getUsuario().getNivelConfianza())
                .reportes(reportesList)
                .confirmadoPorUsuario(verificado)
                .build();
    }

    private ReporteResponse mapearReporteResponse(Reporte reporte) {
        String creadorNombre = reporte.getUsuario().getNombres() + " " +
                (reporte.getUsuario().getApellidos() != null ? reporte.getUsuario().getApellidos() : "");
        return ReporteResponse.builder()
                .id(reporte.getId())
                .creadorNombre(creadorNombre)
                .tipoPeligroNombre(reporte.getTipoPeligro() != null ? reporte.getTipoPeligro().getNombre() : "Incidente")
                .descripcion(reporte.getDescripcion())
                .latitud(reporte.getLatitud())
                .longitud(reporte.getLongitud())
                .fotoUrl(reporte.getFotoUrl())
                .fechaCreacion(reporte.getFechaCreacion())
                .build();
    }

    public static int calcularRadio(int puntos) {
        if (puntos <= 10) return 10;
        if (puntos >= 250) return 130;
        if (puntos < 50) {
            return 10 + (puntos - 10) * (25 - 10) / (50 - 10);
        } else if (puntos < 100) {
            return 25 + (puntos - 50) * (50 - 25) / (100 - 50);
        } else if (puntos < 150) {
            return 50 + (puntos - 100) * (80 - 50) / (150 - 100);
        } else {
            return 80 + (puntos - 150) * (130 - 80) / (250 - 150);
        }
    }

    public static String calcularNivelRiesgo(int puntos) {
        if (puntos <= 30) return "OBSERVACION";
        if (puntos <= 80) return "BAJO";
        if (puntos <= 150) return "MEDIO";
        if (puntos <= 250) return "ALTO";
        return "CRITICO";
    }

    private ComentarioZonaResponse mapearComentarioResponse(ComentarioZona comentario) {
        Usuario usuario = comentario.getUsuario();
        String creadorNombre = usuario.getNombres() + " " + 
                (usuario.getApellidos() != null ? usuario.getApellidos() : "");

        return ComentarioZonaResponse.builder()
                .id(comentario.getId())
                .contenido(comentario.getContenido())
                .calificacion(comentario.getCalificacion())
                .fechaCreacion(comentario.getFechaCreacion())
                .creadorNombre(creadorNombre)
                .creadorFotoPerfil(usuario.getFotoPerfil())
                .fotoUrl(comentario.getFotoUrl())
                .build();
    }

    // =====================================================
    // EVALUAR PROXIMIDAD A ZONAS DE RIESGO
    // =====================================================
    @Transactional
    public com.paralert.dto.response.EvaluacionProximidadResponse evaluarProximidad(
            Usuario usuario, com.paralert.dto.request.EvaluacionProximidadRequest request) {
        
        double userLat = request.getLatitud().doubleValue();
        double userLon = request.getLongitud().doubleValue();

        List<ZonaPeligrosa> zonas = zonaPeligrosaRepository.findAll();
        ZonaPeligrosa zonaMasCercana = null;
        double menorDistancia = Double.MAX_VALUE;

        for (ZonaPeligrosa z : zonas) {
            double dist = calcularDistanciaHaversine(
                    userLat, userLon, 
                    z.getLatitud().doubleValue(), z.getLongitud().doubleValue()
            );
            if (dist < menorDistancia) {
                menorDistancia = dist;
                zonaMasCercana = z;
            }
        }

        String nivelRiesgo = "NINGUNO";
        String mensaje = "Estás en una zona segura";
        Long zonaId = null;
        String zonaNombre = null;
        boolean notificado = false;

        if (usuario != null && !usuario.getAlertasHabilitadas()) {
            return com.paralert.dto.response.EvaluacionProximidadResponse.builder()
                    .nivelRiesgo("NINGUNO")
                    .distanciaMetros(menorDistancia == Double.MAX_VALUE ? null : menorDistancia)
                    .zonaId(null)
                    .zonaNombre(null)
                    .mensaje("Alertas desactivadas")
                    .notificadoContactos(false)
                    .build();
        }

        // Si hay una zona a menos de la distancia configurada (el radio real de la zona!)
        // Wait, under the new model, each zone has a dynamic radius! 
        // So instead of checking <= 50.0, we should check if the distance is <= the zone's radio!
        if (zonaMasCercana != null && menorDistancia <= zonaMasCercana.getRadio()) {
            zonaId = zonaMasCercana.getId();
            zonaNombre = zonaMasCercana.getTitulo();
            nivelRiesgo = zonaMasCercana.getNivelRiesgo().toUpperCase();

            if ("BAJO".equals(nivelRiesgo)) {
                mensaje = "Estás cerca de una zona de peligro bajo: " + zonaMasCercana.getTitulo();
            } else if ("MEDIO".equals(nivelRiesgo) || "ALTO".equals(nivelRiesgo)) {
                boolean enviarEmail = true;

                if (usuario != null) {
                    java.util.Optional<com.paralert.entity.RegistroProximidad> optReg =
                            registroProximidadRepository.findByUsuarioAndZona(usuario, zonaMasCercana);
                    if (optReg.isPresent()) {
                        com.paralert.entity.RegistroProximidad reg = optReg.get();
                        if (reg.getFechaUltimaNotificacion().plusMinutes(10).isAfter(java.time.LocalDateTime.now())) {
                            enviarEmail = false;
                        }
                    }
                }

                if (enviarEmail) {
                    if ("MEDIO".equals(nivelRiesgo)) {
                        mensaje = "Estás cerca de una zona de peligro medio: " + zonaMasCercana.getTitulo() 
                                + ". Se ha alertado a tus contactos de confianza.";
                    } else {
                        mensaje = "¡ALERTA CRÍTICA! Estás ingresando a una zona de peligro alto: " + zonaMasCercana.getTitulo()
                                + ". Se recomienda fuertemente no avanzar. Se ha alertado a tus contactos de confianza.";
                    }
                    
                    if (usuario != null) {
                        List<Amigo> amigos = amigoRepository.findByUsuarioWithAmigoEager(usuario);
                        for (Amigo a : amigos) {
                            try {
                                emailService.enviarAlertaProximidad(
                                        a.getAmigo().getEmail(),
                                        usuario.getNombres() + " " + (usuario.getApellidos() != null ? usuario.getApellidos() : ""),
                                        request.getLatitud(),
                                        request.getLongitud(),
                                        zonaMasCercana.getTitulo(),
                                        nivelRiesgo
                                );
                            } catch (Exception e) {
                                log.error("Error enviando alerta de proximidad por email: {}", e.getMessage());
                            }
                        }

                        // Guardar o actualizar registro de proximidad
                        com.paralert.entity.RegistroProximidad reg = registroProximidadRepository.findByUsuarioAndZona(usuario, zonaMasCercana)
                                .orElse(com.paralert.entity.RegistroProximidad.builder()
                                        .usuario(usuario)
                                        .zona(zonaMasCercana)
                                        .build());
                        reg.setFechaUltimaNotificacion(java.time.LocalDateTime.now());
                        registroProximidadRepository.save(reg);

                        notificado = true;
                    }
                } else {
                    if ("MEDIO".equals(nivelRiesgo)) {
                        mensaje = "Estás cerca de una zona de peligro medio: " + zonaMasCercana.getTitulo();
                    } else {
                        mensaje = "¡ALERTA CRÍTICA! Estás ingresando a una zona de peligro alto: " + zonaMasCercana.getTitulo()
                                + ". Se recomienda fuertemente no avanzar.";
                    }
                }
            }
        }

        return com.paralert.dto.response.EvaluacionProximidadResponse.builder()
                .nivelRiesgo(nivelRiesgo)
                .distanciaMetros(menorDistancia == Double.MAX_VALUE ? null : menorDistancia)
                .zonaId(zonaId)
                .zonaNombre(zonaNombre)
                .mensaje(mensaje)
                .notificadoContactos(notificado)
                .build();
    }

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radio de la Tierra en metros
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
