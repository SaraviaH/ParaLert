package com.paralert.controller;

import com.paralert.dto.response.AdminStatsResponse;
import com.paralert.dto.response.AdminUserResponse;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.IaAnalysisResponse;
import com.paralert.entity.ComentarioZona;
import com.paralert.entity.Rol;
import com.paralert.entity.TipoPeligro;
import com.paralert.entity.Usuario;
import com.paralert.entity.Reporte;
import com.paralert.entity.ZonaPeligrosa;
import com.paralert.entity.Confirmacion;
import com.paralert.entity.RegistroProximidad;
import com.paralert.entity.SosAlerta;
import com.paralert.entity.PalabraSospechosa;
import com.paralert.entity.enums.EstadoUsuario;
import com.paralert.repository.*;
import com.paralert.service.ZonaPeligrosaService;
import com.paralert.service.EmailService;
import com.paralert.service.IaAnalysisService;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ZonaPeligrosaService zonaPeligrosaService;
    private final UsuarioRepository usuarioRepository;
    private final ZonaPeligrosaRepository zonaPeligrosaRepository;
    private final ComentarioZonaRepository comentarioZonaRepository;
    private final SosAlertaRepository sosAlertaRepository;
    private final TipoPeligroRepository tipoPeligroRepository;
    private final RegistroProximidadRepository registroProximidadRepository;
    private final EmailService emailService;
    private final IaAnalysisService iaAnalysisService;
    private final ReporteRepository reporteRepository;
    private final ConfirmacionRepository confirmacionRepository;
    private final AmigoRepository amigoRepository;
    private final SolicitudAmistadRepository solicitudAmistadRepository;
    private final CodigoVerificacionRepository codigoVerificacionRepository;
    private final AlertaEnviadaRepository alertaEnviadaRepository;
    private final PalabraSospechosaRepository palabraSospechosaRepository;


    // =====================================================
    // ESTADÍSTICAS DASHBOARD
    // GET /api/admin/estadisticas
    // =====================================================
    @GetMapping("/estadisticas")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminStatsResponse> obtenerEstadisticas() {
        long totalUsuarios = usuarioRepository.count();
        long totalZonas = zonaPeligrosaRepository.count();
        long totalComentarios = comentarioZonaRepository.count();
        long totalSos = sosAlertaRepository.count();
        long totalTipos = tipoPeligroRepository.count();
        long totalAfectados = registroProximidadRepository.count();

        // 1. Distribución por Nivel de Riesgo (en base de datos)
        Map<String, Long> distribucionRiesgo = zonaPeligrosaRepository.countZonesByNivelRiesgo().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? ((String) row[0]).toUpperCase() : "OBSERVACION",
                        row -> (Long) row[1]
                ));

        // 2. Distribución por Categorías de Peligro (en base de datos)
        Map<String, Long> distribucionCategoria = zonaPeligrosaRepository.countZonesByTipoPeligro().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? (String) row[0] : "Sin Categoría",
                        row -> (Long) row[1]
                ));

        // 3. Obtener Zonas Calientes (en base de datos con paginación nativa)
        List<com.paralert.dto.response.ZonaCalienteResponse> zonasCalientes = registroProximidadRepository
                .findTopHotZones(org.springframework.data.domain.PageRequest.of(0, 5)).stream()
                .map(row -> com.paralert.dto.response.ZonaCalienteResponse.builder()
                        .zonaId((Long) row[0])
                        .zonaNombre((String) row[1])
                        .nivelRiesgo((String) row[2])
                        .cantidadNotificaciones((Long) row[3])
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(AdminStatsResponse.builder()
                .totalUsuarios(totalUsuarios)
                .totalZonas(totalZonas)
                .totalComentarios(totalComentarios)
                .totalSosActivos(totalSos)
                .totalTiposPeligro(totalTipos)
                .totalAfectados(totalAfectados)
                .distribucionRiesgo(distribucionRiesgo)
                .distribucionCategoria(distribucionCategoria)
                .zonasCalientes(zonasCalientes)
                .build());
    }

    // =====================================================
    // LISTAR TODOS LOS USUARIOS
    // GET /api/admin/usuarios
    // =====================================================
    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUserResponse>> listarUsuarios() {
        List<AdminUserResponse> usuarios = usuarioRepository.findAll().stream()
                .map(u -> AdminUserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .username(u.getUsername())
                        .nombres(u.getNombres())
                        .apellidos(u.getApellidos())
                        .dni(u.getDni())
                        .telefono(u.getTelefono())
                        .estado(u.getEstado().name())
                        .nivelConfianza(u.getNivelConfianza())
                        .verificado(u.getVerificado())
                        .fechaRegistro(u.getFechaRegistro())
                        .roles(u.getRoles().stream().map(Rol::getNombre).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    // =====================================================
    // CAMBIAR ESTADO DE USUARIO (BLOQUEAR/DESBLOQUEAR)
    // PUT /api/admin/usuarios/{id}/estado
    // =====================================================
    @PutMapping("/usuarios/{id}/estado")
    @Transactional
    public ResponseEntity<MessageResponse> cambiarEstadoUsuario(
            @PathVariable Long id,
            @RequestParam("estado") String estadoStr) {
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        try {
            EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(estadoStr.toUpperCase());
            usuario.setEstado(nuevoEstado);
            usuarioRepository.save(usuario);
            return ResponseEntity.ok(new MessageResponse("Estado del usuario modificado a: " + nuevoEstado.name()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido. Usa: ACTIVO, BLOQUEADO, INACTIVO o ELIMINADO");
        }
    }

    // =====================================================
    // ELIMINAR USUARIO FISICAMENTE (LIMPIEZA DE DEPENDENCIAS)
    // DELETE /api/admin/usuarios/{id}
    // =====================================================
    @DeleteMapping("/usuarios/{id}")
    @Transactional
    public ResponseEntity<MessageResponse> eliminarUsuario(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        try {
            // 1. Solicitudes de amistad
            solicitudAmistadRepository.deleteByEmisorOrReceptor(usuario, usuario);

            // 2. Relaciones de amistad (amigos)
            amigoRepository.deleteByUsuarioOrAmigo(usuario, usuario);

            // 3. Códigos de verificación
            codigoVerificacionRepository.deleteByUsuario(usuario);

            // 4. Confirmaciones de zonas
            confirmacionRepository.deleteByUsuario(usuario);

            // 5. Comentarios de zonas
            comentarioZonaRepository.deleteByUsuario(usuario);

            // 6. Reportes de incidentes
            reporteRepository.deleteByUsuario(usuario);

            // 7. Registros de proximidad asociados al usuario
            registroProximidadRepository.deleteByUsuario(usuario);

            // 8. Registros de proximidad asociados a zonas creadas por el usuario
            List<ZonaPeligrosa> zonasCreadas = zonaPeligrosaRepository.findByUsuario(usuario);
            if (!zonasCreadas.isEmpty()) {
                registroProximidadRepository.deleteByZonaIn(zonasCreadas);
            }

            // 9. Alertas enviadas donde el usuario es contacto
            alertaEnviadaRepository.deleteByContacto(usuario);

            // 10. Alertas SOS creadas por el usuario (cascada elimina evidencias y enviadas asociadas)
            List<SosAlerta> alertas = sosAlertaRepository.findByUsuario(usuario);
            sosAlertaRepository.deleteAll(alertas);

            // 11. Zonas peligrosas creadas por el usuario (cascada elimina comentarios, reportes, confirmaciones)
            if (!zonasCreadas.isEmpty()) {
                zonaPeligrosaRepository.deleteAll(zonasCreadas);
            }

            // 12. Eliminar el usuario (cascada elimina usuario_roles)
            usuarioRepository.delete(usuario);

            return ResponseEntity.ok(new MessageResponse("Usuario y todas sus dependencias fueron eliminados correctamente de forma física."));
        } catch (Exception e) {
            // Registrar error para auditoría
            System.err.println("Error al eliminar usuario " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al intentar eliminar físicamente al usuario: " + e.getMessage());
        }
    }

    // =====================================================
    // ELIMINAR COMENTARIO
    // DELETE /api/admin/comentarios/{id}
    // =====================================================
    @DeleteMapping("/comentarios/{id}")
    @Transactional
    public ResponseEntity<MessageResponse> eliminarComentario(@PathVariable Long id) {
        ComentarioZona comentario = comentarioZonaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));
        ZonaPeligrosa zona = comentario.getZona();
        if (zona != null) {
            int nuevosPuntos = Math.max(0, (zona.getPuntaje() != null ? zona.getPuntaje() : 10) - 2);
            zona.setPuntaje(nuevosPuntos);
            zona.setRadio(ZonaPeligrosaService.calcularRadio(nuevosPuntos));
            zona.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(nuevosPuntos));
            if ("ACTIVA".equals(zona.getEstado()) && nuevosPuntos <= 30) {
                zona.setEstado("OBSERVACION");
            }
            zonaPeligrosaRepository.save(zona);
        }
        comentarioZonaRepository.delete(comentario);
        return ResponseEntity.ok(new MessageResponse("Comentario eliminado correctamente y puntaje de la zona actualizado"));
    }

    // =====================================================
    // ELIMINAR ZONA DE RIESGO
    // DELETE /api/admin/zonas/{id}
    // =====================================================
    @DeleteMapping("/zonas/{id}")
    public ResponseEntity<MessageResponse> eliminarZona(@PathVariable Long id) {
        zonaPeligrosaService.eliminarZona(id);
        return ResponseEntity.ok(new MessageResponse("Zona peligrosa eliminada correctamente por el administrador"));
    }

    // =====================================================
    // CREAR NUEVO TIPO DE PELIGRO
    // POST /api/admin/tipos-peligro
    // =====================================================
    @PostMapping("/tipos-peligro")
    @Transactional
    public ResponseEntity<TipoPeligro> crearTipoPeligro(@RequestBody Map<String, String> request) {
        String nombre = request.get("nombre");
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del tipo de peligro es obligatorio");
        }

        nombre = nombre.trim();
        if (tipoPeligroRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un tipo de peligro con ese nombre");
        }

        TipoPeligro tp = TipoPeligro.builder()
                .nombre(nombre)
                .build();
        return ResponseEntity.ok(tipoPeligroRepository.save(tp));
    }

    // =====================================================
    // ELIMINAR TIPO DE PELIGRO
    // DELETE /api/admin/tipos-peligro/{id}
    // =====================================================
    @DeleteMapping("/tipos-peligro/{id}")
    @Transactional
    public ResponseEntity<MessageResponse> eliminarTipoPeligro(@PathVariable Long id) {
        TipoPeligro tp = tipoPeligroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de peligro no encontrado"));
        
        tipoPeligroRepository.delete(tp);
        return ResponseEntity.ok(new MessageResponse("Tipo de peligro eliminado correctamente"));
    }

    // =====================================================
    // ENVIAR ADVERTENCIA A USUARIO POR MAL USO
    // POST /api/admin/usuarios/{id}/advertir
    // =====================================================
    @PostMapping("/usuarios/{id}/advertir")
    @Transactional
    public ResponseEntity<MessageResponse> advertirUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String motivo = requestBody.get("motivo");
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de la advertencia es obligatorio");
        }
        motivo = motivo.trim();

        // Aplicar penalización: +1 advertencia y -10 puntos de confianza
        int warnings = (usuario.getCantidadAdvertencias() != null ? usuario.getCantidadAdvertencias() : 0) + 1;
        int confianza = Math.max(0, (usuario.getNivelConfianza() != null ? usuario.getNivelConfianza() : 0) - 10);

        usuario.setCantidadAdvertencias(warnings);
        usuario.setNivelConfianza(confianza);

        boolean debeBloquearse = (warnings >= 2) || (confianza <= 0);

        if (debeBloquearse) {
            usuario.setEstado(EstadoUsuario.BLOQUEADO);
            usuarioRepository.save(usuario);
            
            // Enviar correo de bloqueo
            emailService.enviarBloqueoCuenta(
                    usuario.getEmail(),
                    usuario.getNombres() + " " + (usuario.getApellidos() != null ? usuario.getApellidos() : ""),
                    motivo,
                    confianza,
                    warnings
            );
            return ResponseEntity.ok(new MessageResponse("El usuario ha acumulado infracciones y ha sido BLOQUEADO. Correo enviado."));
        } else {
            usuarioRepository.save(usuario);

            // Enviar correo de advertencia
            emailService.enviarAdvertenciaMalUso(
                    usuario.getEmail(),
                    usuario.getNombres() + " " + (usuario.getApellidos() != null ? usuario.getApellidos() : ""),
                    motivo,
                    confianza,
                    warnings
            );
            return ResponseEntity.ok(new MessageResponse("Advertencia enviada. Confianza disminuida en 10 puntos. Correo enviado."));
        }
    }

    // =====================================================
    // EXPORTAR REPORTE ESTADÍSTICO EN PDF
    // GET /api/admin/reporte-pdf
    // =====================================================
    @GetMapping("/reporte-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportarReportePdf() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Cabecera / Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, java.awt.Color.WHITE);
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 12, java.awt.Color.LIGHT_GRAY);
            Font fontSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new java.awt.Color(30, 41, 59));
            Font fontBodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new java.awt.Color(51, 65, 85));
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, new java.awt.Color(71, 85, 105));

            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell cellHeader = new PdfPCell();
            cellHeader.setBackgroundColor(new java.awt.Color(26, 82, 118)); // Azul oscuro elegante
            cellHeader.setPadding(20);
            cellHeader.setBorder(Rectangle.NO_BORDER);

            Paragraph pTitle = new Paragraph("PARALERT - CONSOLA DE ADMINISTRACIÓN", fontTitulo);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            cellHeader.addElement(pTitle);

            Paragraph pSub = new Paragraph("Reporte Estadístico Integral de Seguridad Ciudadana", fontSub);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingBefore(5);
            cellHeader.addElement(pSub);

            headerTable.addCell(cellHeader);
            document.add(headerTable);

            // Separador
            Paragraph pFecha = new Paragraph("Fecha de generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), fontBody);
            pFecha.setAlignment(Element.ALIGN_RIGHT);
            pFecha.setSpacingBefore(10);
            pFecha.setSpacingAfter(20);
            document.add(pFecha);

            // 2. Sección: KPIs Generales
            document.add(new Paragraph("I. Resumen General del Sistema", fontSeccion));
            document.add(new Paragraph(" ", fontBody));

            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(20);

            long totalUsers = usuarioRepository.count();
            long totalZonas = zonaPeligrosaRepository.count();
            long totalComments = comentarioZonaRepository.count();
            long totalAfectados = registroProximidadRepository.count();

            agregarCeldaKpi(kpiTable, "Usuarios", String.valueOf(totalUsers), fontBodyBold, fontBody);
            agregarCeldaKpi(kpiTable, "Reportes Mapa", String.valueOf(totalZonas), fontBodyBold, fontBody);
            agregarCeldaKpi(kpiTable, "Testimonios", String.valueOf(totalComments), fontBodyBold, fontBody);
            agregarCeldaKpi(kpiTable, "Afectados/Alertados", String.valueOf(totalAfectados), fontBodyBold, fontBody);

            document.add(kpiTable);

            // 3. Sección: Zonas Críticas de Tránsito Recurrente
            document.add(new Paragraph("II. Zonas Críticas de Tránsito Recurrente", fontSeccion));
            document.add(new Paragraph(" ", fontBody));

            PdfPTable hotZonesTable = new PdfPTable(3);
            hotZonesTable.setWidthPercentage(100);
            hotZonesTable.setSpacingAfter(20);
            hotZonesTable.setWidths(new float[]{4f, 2f, 2f});

            agregarCeldaCabecera(hotZonesTable, "Sector de Riesgo", fontBodyBold);
            agregarCeldaCabecera(hotZonesTable, "Nivel de Peligro", fontBodyBold);
            agregarCeldaCabecera(hotZonesTable, "Alertas de Proximidad", fontBodyBold);

            List<com.paralert.dto.response.ZonaCalienteResponse> zonasCalientes = registroProximidadRepository
                    .findTopHotZones(org.springframework.data.domain.PageRequest.of(0, 5)).stream()
                    .map(row -> com.paralert.dto.response.ZonaCalienteResponse.builder()
                            .zonaId((Long) row[0])
                            .zonaNombre((String) row[1])
                            .nivelRiesgo((String) row[2])
                            .cantidadNotificaciones((Long) row[3])
                            .build())
                    .collect(Collectors.toList());

            if (zonasCalientes.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Paragraph("No se registran tránsitos peligrosos preventivos aún.", fontBody));
                emptyCell.setColspan(3);
                emptyCell.setPadding(10);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                hotZonesTable.addCell(emptyCell);
            } else {
                for (com.paralert.dto.response.ZonaCalienteResponse z : zonasCalientes) {
                    hotZonesTable.addCell(new PdfPCell(new Paragraph(z.getZonaNombre(), fontBody)));
                    hotZonesTable.addCell(new PdfPCell(new Paragraph(z.getNivelRiesgo(), fontBody)));
                    hotZonesTable.addCell(new PdfPCell(new Paragraph(String.valueOf(z.getCantidadNotificaciones()), fontBody)));
                }
            }
            document.add(hotZonesTable);

            // 4. Sección: Zonas Peligrosas Activas
            document.add(new Paragraph("III. Reportes de Peligro Activos en el Mapa", fontSeccion));
            document.add(new Paragraph(" ", fontBody));

            PdfPTable zonesTable = new PdfPTable(4);
            zonesTable.setWidthPercentage(100);
            zonesTable.setSpacingAfter(20);
            zonesTable.setWidths(new float[]{4f, 2f, 1f, 1f});

            agregarCeldaCabecera(zonesTable, "Zona de Peligro", fontBodyBold);
            agregarCeldaCabecera(zonesTable, "Riesgo", fontBodyBold);
            agregarCeldaCabecera(zonesTable, "Estrellas", fontBodyBold);
            agregarCeldaCabecera(zonesTable, "Opiniones", fontBodyBold);

            List<com.paralert.dto.response.ZonaPeligrosaResponse> zonas = zonaPeligrosaService.obtenerTodasLasZonas();
            for (com.paralert.dto.response.ZonaPeligrosaResponse z : zonas) {
                zonesTable.addCell(new PdfPCell(new Paragraph(z.getTitulo(), fontBody)));
                zonesTable.addCell(new PdfPCell(new Paragraph(z.getNivelRiesgo(), fontBody)));
                zonesTable.addCell(new PdfPCell(new Paragraph(String.format("%.1f★", z.getCalificacionPromedio()), fontBody)));
                zonesTable.addCell(new PdfPCell(new Paragraph(String.valueOf(z.getTotalComentarios()), fontBody)));
            }
            document.add(zonesTable);

            // 5. Sección: Tabla de Usuarios y Puntos de Confianza
            document.add(new Paragraph("IV. Estado de Cuentas y Puntos de Confianza", fontSeccion));
            document.add(new Paragraph(" ", fontBody));

            PdfPTable usersTable = new PdfPTable(4);
            usersTable.setWidthPercentage(100);
            usersTable.setWidths(new float[]{3f, 2f, 1.5f, 1.5f});

            agregarCeldaCabecera(usersTable, "Usuario / Correo", fontBodyBold);
            agregarCeldaCabecera(usersTable, "Nombres", fontBodyBold);
            agregarCeldaCabecera(usersTable, "Confianza", fontBodyBold);
            agregarCeldaCabecera(usersTable, "Advertencias", fontBodyBold);

            List<Usuario> usuarios = usuarioRepository.findAll();
            for (Usuario u : usuarios) {
                usersTable.addCell(new PdfPCell(new Paragraph(u.getEmail(), fontBody)));
                usersTable.addCell(new PdfPCell(new Paragraph(u.getNombres() + " " + (u.getApellidos() != null ? u.getApellidos() : ""), fontBody)));
                usersTable.addCell(new PdfPCell(new Paragraph(u.getNivelConfianza() + "/100", fontBody)));
                usersTable.addCell(new PdfPCell(new Paragraph(String.valueOf(u.getCantidadAdvertencias() != null ? u.getCantidadAdvertencias() : 0), fontBody)));
            }
            document.add(usersTable);

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_paralert_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(out.toByteArray());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AdminController.class).error("Error generando reporte PDF administrativo: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================
    // OBTENER REPORTE IA DE ANÁLISIS DE PATRONES
    // GET /api/admin/ia-analisis
    // =====================================================
    @GetMapping("/ia-analisis")
    public ResponseEntity<IaAnalysisResponse> obtenerReporteIa() {
        return ResponseEntity.ok(iaAnalysisService.generarReporteIa());
    }

    // =====================================================
    // ELIMINAR UN REPORTE
    // DELETE /api/admin/reportes/{id}
    // =====================================================
    @DeleteMapping("/reportes/{id}")
    @Transactional
    public ResponseEntity<MessageResponse> eliminarReporte(@PathVariable Long id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        ZonaPeligrosa zona = reporte.getZona();
        if (zona != null) {
            int nuevosPuntos = Math.max(0, (zona.getPuntaje() != null ? zona.getPuntaje() : 10) - 10);
            zona.setPuntaje(nuevosPuntos);
            zona.setRadio(ZonaPeligrosaService.calcularRadio(nuevosPuntos));
            zona.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(nuevosPuntos));
            if ("ACTIVA".equals(zona.getEstado()) && nuevosPuntos <= 30) {
                zona.setEstado("OBSERVACION");
            }
            zonaPeligrosaRepository.save(zona);
        }
        reporteRepository.delete(reporte);
        return ResponseEntity.ok(new MessageResponse("Reporte eliminado correctamente y puntos restados de la zona"));
    }

    // =====================================================
    // AJUSTAR PUNTOS DE FORMA MANUAL
    // POST /api/admin/zonas/ajustar-puntos
    // =====================================================
    @PostMapping("/zonas/ajustar-puntos")
    @Transactional
    public ResponseEntity<MessageResponse> ajustarPuntos(
            @RequestParam("zonaId") Long zonaId,
            @RequestParam("puntos") int puntos) {
        ZonaPeligrosa zona = zonaPeligrosaRepository.findById(zonaId)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));
        zona.setPuntaje(puntos);
        zona.setRadio(ZonaPeligrosaService.calcularRadio(puntos));
        zona.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(puntos));
        if (puntos <= 30) {
            zona.setEstado("OBSERVACION");
        } else {
            zona.setEstado("ACTIVA");
        }
        zonaPeligrosaRepository.save(zona);
        return ResponseEntity.ok(new MessageResponse("Puntaje de la zona actualizado a: " + puntos));
    }

    // =====================================================
    // MODIFICAR ESTADO DE ZONA MANUALMENTE
    // POST /api/admin/zonas/{id}/estado
    // =====================================================
    @PostMapping("/zonas/{id}/estado")
    @Transactional
    public ResponseEntity<MessageResponse> cambiarEstadoZona(
            @PathVariable Long id,
            @RequestParam("estado") String estado) {
        ZonaPeligrosa zona = zonaPeligrosaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));
        zona.setEstado(estado.toUpperCase());
        zonaPeligrosaRepository.save(zona);
        return ResponseEntity.ok(new MessageResponse("Estado de la zona actualizado a: " + estado.toUpperCase()));
    }

    // =====================================================
    // FUSIONAR ZONAS DUPLICADAS
    // POST /api/admin/zonas/fusionar
    // =====================================================
    @PostMapping("/zonas/fusionar")
    @Transactional
    public ResponseEntity<MessageResponse> fusionarZonas(
            @RequestParam("zonaId1") Long zonaId1,
            @RequestParam("zonaId2") Long zonaId2) {
        if (zonaId1.equals(zonaId2)) {
            throw new IllegalArgumentException("No se puede fusionar una zona consigo misma");
        }

        ZonaPeligrosa zona1 = zonaPeligrosaRepository.findById(zonaId1)
                .orElseThrow(() -> new IllegalArgumentException("Zona de destino no encontrada"));
        ZonaPeligrosa zona2 = zonaPeligrosaRepository.findById(zonaId2)
                .orElseThrow(() -> new IllegalArgumentException("Zona duplicada no encontrada"));

        // 1. Migrar reportes
        for (Reporte r : new ArrayList<>(zona2.getReportes())) {
            r.setZona(zona1);
            reporteRepository.save(r);
        }

        // 2. Migrar comentarios
        for (ComentarioZona c : new ArrayList<>(zona2.getComentarios())) {
            c.setZona(zona1);
            comentarioZonaRepository.save(c);
        }

        // 3. Migrar confirmaciones (evitando violaciones de clave única)
        for (Confirmacion c : new ArrayList<>(zona2.getConfirmaciones())) {
            if (confirmacionRepository.existsByUsuarioAndZona(c.getUsuario(), zona1)) {
                confirmacionRepository.delete(c);
            } else {
                c.setZona(zona1);
                confirmacionRepository.save(c);
            }
        }

        // 4. Migrar registros de proximidad (evitando violaciones de clave única)
        List<RegistroProximidad> proximidades2 = registroProximidadRepository.findByZona(zona2);
        for (RegistroProximidad rp : proximidades2) {
            if (registroProximidadRepository.findByUsuarioAndZona(rp.getUsuario(), zona1).isPresent()) {
                registroProximidadRepository.delete(rp);
            } else {
                rp.setZona(zona1);
                registroProximidadRepository.save(rp);
            }
        }

        // 5. Sumar puntajes de ambas zonas
        int nuevosPuntos = (zona1.getPuntaje() != null ? zona1.getPuntaje() : 10) +
                           (zona2.getPuntaje() != null ? zona2.getPuntaje() : 10);
        zona1.setPuntaje(nuevosPuntos);
        zona1.setRadio(ZonaPeligrosaService.calcularRadio(nuevosPuntos));
        zona1.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(nuevosPuntos));
        if (nuevosPuntos > 30) {
            zona1.setEstado("ACTIVA");
        } else {
            zona1.setEstado("OBSERVACION");
        }
        zonaPeligrosaRepository.save(zona1);

        // 6. Eliminar zona2
        zonaPeligrosaRepository.delete(zona2);

        return ResponseEntity.ok(new MessageResponse("Zonas fusionadas exitosamente en la zona #" + zonaId1));
    }

    private void agregarCeldaKpi(PdfPTable table, String titulo, String valor, Font fontBold, Font fontRegular) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new java.awt.Color(248, 250, 252));
        cell.setBorderColor(new java.awt.Color(226, 232, 240));
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pTitle = new Paragraph(titulo.toUpperCase(), fontRegular);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pTitle);

        Paragraph pVal = new Paragraph(valor, fontBold);
        pVal.setAlignment(Element.ALIGN_CENTER);
        pVal.setSpacingBefore(4);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private void agregarCeldaCabecera(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(new java.awt.Color(241, 245, 249));
        cell.setBorderColor(new java.awt.Color(203, 213, 225));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    // =====================================================
    // APROBAR COMENTARIO SOSPECHOSO
    // POST /api/admin/comentarios/{id}/aprobar
    // =====================================================
    @PostMapping("/comentarios/{id}/aprobar")
    @Transactional
    public ResponseEntity<MessageResponse> aprobarComentario(@PathVariable Long id) {
        ComentarioZona comentario = comentarioZonaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));
        
        if (Boolean.TRUE.equals(comentario.getSospechoso())) {
            comentario.setSospechoso(false);
            comentario.setJuicioTipo("APROBADO");
            comentario.setJuicioJustificacion("Aprobado manualmente por el administrador.");
            comentario.setFechaEvaluacion(LocalDateTime.now());
            comentario.setEvaluador("MODERADO_MANUAL");
            comentarioZonaRepository.save(comentario);

            // Añadir los 2 puntos de veracidad a la zona correspondientes a este comentario
            ZonaPeligrosa zona = comentario.getZona();
            if (zona != null) {
                int nuevosPuntos = (zona.getPuntaje() != null ? zona.getPuntaje() : 10) + 2;
                zona.setPuntaje(nuevosPuntos);
                zona.setRadio(ZonaPeligrosaService.calcularRadio(nuevosPuntos));
                zona.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(nuevosPuntos));
                if ("OBSERVACION".equals(zona.getEstado()) && nuevosPuntos > 30) {
                    zona.setEstado("ACTIVA");
                }
                zonaPeligrosaRepository.save(zona);
            }
        }
        return ResponseEntity.ok(new MessageResponse("Comentario aprobado y puntos de la zona restaurados."));
    }

    // =====================================================
    // APROBAR REPORTE SOSPECHOSO
    // POST /api/admin/reportes/{id}/aprobar
    // =====================================================
    @PostMapping("/reportes/{id}/aprobar")
    @Transactional
    public ResponseEntity<MessageResponse> aprobarReporte(@PathVariable Long id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        
        if (Boolean.TRUE.equals(reporte.getSospechoso())) {
            reporte.setSospechoso(false);
            reporte.setJuicioTipo("APROBADO");
            reporte.setJuicioJustificacion("Aprobado manualmente por el administrador.");
            reporte.setFechaEvaluacion(LocalDateTime.now());
            reporte.setEvaluador("MODERADO_MANUAL");
            reporteRepository.save(reporte);

            // Añadir los 10 puntos de veracidad a la zona correspondientes a este reporte
            ZonaPeligrosa zona = reporte.getZona();
            if (zona != null) {
                int nuevosPuntos = (zona.getPuntaje() != null ? zona.getPuntaje() : 10) + 10;
                zona.setPuntaje(nuevosPuntos);
                zona.setRadio(ZonaPeligrosaService.calcularRadio(nuevosPuntos));
                zona.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(nuevosPuntos));
                if ("OBSERVACION".equals(zona.getEstado()) && nuevosPuntos > 30) {
                    zona.setEstado("ACTIVA");
                }
                zonaPeligrosaRepository.save(zona);
            }
        }
        return ResponseEntity.ok(new MessageResponse("Reporte aprobado y puntos de la zona restaurados."));
    }

    // =====================================================
    // GESTIÓN DEL DICCIONARIO: LISTAR PALABRAS
    // GET /api/admin/diccionario
    // =====================================================
    @GetMapping("/diccionario")
    public ResponseEntity<List<PalabraSospechosa>> listarDiccionario() {
        return ResponseEntity.ok(palabraSospechosaRepository.findAll());
    }

    // =====================================================
    // GESTIÓN DEL DICCIONARIO: AGREGAR PALABRA
    // POST /api/admin/diccionario
    // =====================================================
    @PostMapping("/diccionario")
    @Transactional
    public ResponseEntity<PalabraSospechosa> agregarPalabraDiccionario(@RequestBody Map<String, String> request) {
        String palabra = request.get("palabra");
        String categoria = request.get("categoria");

        if (palabra == null || palabra.trim().isEmpty()) {
            throw new IllegalArgumentException("La palabra es obligatoria");
        }
        if (categoria == null || categoria.trim().isEmpty()) {
            categoria = "FALSO";
        }

        palabra = palabra.trim().toLowerCase();
        categoria = categoria.trim().toUpperCase();

        if (palabraSospechosaRepository.existsByPalabra(palabra)) {
            throw new IllegalArgumentException("La palabra ya existe en el diccionario");
        }

        PalabraSospechosa ps = PalabraSospechosa.builder()
                .palabra(palabra)
                .categoria(categoria)
                .fechaCreado(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(palabraSospechosaRepository.save(ps));
    }

    // =====================================================
    // GESTIÓN DEL DICCIONARIO: ELIMINAR PALABRA
    // DELETE /api/admin/diccionario/{id}
    // =====================================================
    @DeleteMapping("/diccionario/{id}")
    @Transactional
    public ResponseEntity<MessageResponse> eliminarPalabraDiccionario(@PathVariable Long id) {
        PalabraSospechosa ps = palabraSospechosaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Palabra no encontrada"));
        palabraSospechosaRepository.delete(ps);
        return ResponseEntity.ok(new MessageResponse("Palabra eliminada del diccionario correctamente"));
    }
}
