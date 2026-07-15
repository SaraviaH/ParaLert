package com.paralert.service;

import com.paralert.dto.response.IaAnalysisResponse;
import com.paralert.entity.ComentarioZona;
import com.paralert.entity.PalabraSospechosa;
import com.paralert.entity.Reporte;
import com.paralert.entity.ZonaPeligrosa;
import com.paralert.repository.ComentarioZonaRepository;
import com.paralert.repository.PalabraSospechosaRepository;
import com.paralert.repository.ReporteRepository;
import com.paralert.repository.ZonaPeligrosaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IaAnalysisService {

    private final ZonaPeligrosaRepository zonaPeligrosaRepository;
    private final ReporteRepository reporteRepository;
    private final ComentarioZonaRepository comentarioZonaRepository;
    private final PalabraSospechosaRepository palabraSospechosaRepository;

    @PostConstruct
    @Transactional
    public void inicializarDiccionario() {
        if (palabraSospechosaRepository.count() == 0) {
            log.info("Inicializando diccionario de palabras sospechosas e insultos desde archivo local...");
            try {
                ClassPathResource resource = new ClassPathResource("palabras_sospechosas.txt");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            String palabra = parts[0].trim().toLowerCase();
                            String categoria = parts[1].trim().toUpperCase();
                            if (!palabraSospechosaRepository.existsByPalabra(palabra)) {
                                palabraSospechosaRepository.save(PalabraSospechosa.builder()
                                        .palabra(palabra)
                                        .categoria(categoria)
                                        .fechaCreado(LocalDateTime.now())
                                        .build());
                            }
                        }
                    }
                }
                log.info("Diccionario de moderación local inicializado con éxito.");
            } catch (Exception e) {
                log.error("Error al inicializar el diccionario de palabras sospechosas: ", e);
            }
        }
    }

    public static class AnalisisResultado {
        private final boolean sospechoso;
        private final String categoria;
        private final String justificacion;

        public AnalisisResultado(boolean sospechoso, String categoria, String justificacion) {
            this.sospechoso = sospechoso;
            this.categoria = categoria;
            this.justificacion = justificacion;
        }

        public boolean isSospechoso() { return sospechoso; }
        public String getCategoria() { return categoria; }
        public String getJustificacion() { return justificacion; }
    }

    @Transactional(readOnly = true)
    public AnalisisResultado analizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return new AnalisisResultado(false, "APROBADO", "Texto vacío o nulo.");
        }

        String textoLimpio = texto.toLowerCase()
                .replaceAll("[.,\\/#!$%\\^&\\*;:{}=\\-_`~()¡?¿\"'’\\-\\[\\]]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        List<PalabraSospechosa> diccionario = palabraSospechosaRepository.findAll();

        String[] palabrasTexto = textoLimpio.split(" ");
        Set<String> setPalabrasTexto = new HashSet<>(Arrays.asList(palabrasTexto));

        for (PalabraSospechosa ps : diccionario) {
            String termino = ps.getPalabra().trim().toLowerCase();

            if (termino.contains(" ")) {
                if (textoLimpio.contains(termino)) {
                    return new AnalisisResultado(
                            true,
                            ps.getCategoria(),
                            "Contiene frase no permitida: '" + ps.getPalabra() + "' (Categoría: " + ps.getCategoria() + ")"
                    );
                }
            } else {
                if (setPalabrasTexto.contains(termino)) {
                    return new AnalisisResultado(
                            true,
                            ps.getCategoria(),
                            "Contiene palabra no permitida: '" + ps.getPalabra() + "' (Categoría: " + ps.getCategoria() + ")"
                    );
                }
            }
        }

        return new AnalisisResultado(false, "APROBADO", "No se detectaron palabras prohibidas o sospechosas.");
    }

    @Transactional
    public void analizarComentario(ComentarioZona comentario) {
        AnalisisResultado resultado = analizarTexto(comentario.getContenido());
        comentario.setSospechoso(resultado.isSospechoso());
        comentario.setJuicioTipo(resultado.getCategoria());
        comentario.setJuicioJustificacion(resultado.getJustificacion());
        comentario.setEvaluador("DICCIONARIO_LOCAL");
        comentario.setFechaEvaluacion(LocalDateTime.now());
        comentarioZonaRepository.save(comentario);
        log.info("Comentario #{} moderado localmente. Sospechoso: {}. Categoría: {}", 
                comentario.getId(), resultado.isSospechoso(), resultado.getCategoria());
    }

    @Transactional
    public void analizarReporte(Reporte reporte) {
        AnalisisResultado resultado = analizarTexto(reporte.getDescripcion());
        reporte.setSospechoso(resultado.isSospechoso());
        reporte.setJuicioTipo(resultado.getCategoria());
        reporte.setJuicioJustificacion(resultado.getJustificacion());
        reporte.setEvaluador("DICCIONARIO_LOCAL");
        reporte.setFechaEvaluacion(LocalDateTime.now());
        reporteRepository.save(reporte);
        log.info("Reporte #{} moderado localmente. Sospechoso: {}. Categoría: {}", 
                reporte.getId(), resultado.isSospechoso(), resultado.getCategoria());
    }

    @Transactional(readOnly = true)
    public IaAnalysisResponse generarReporteIa() {
        log.info("Iniciando análisis de patrones mediante IA/Diccionario...");

        LocalDateTime limiteReciente = LocalDateTime.now().minusDays(3);

        // 1. Detección de Zonas con Crecimiento Acelerado
        List<ZonaPeligrosa> zonasFiltradas = zonaPeligrosaRepository.findZonesWithHighRecentActivity(limiteReciente);
        List<Map<String, Object>> crecimientoZonas = new ArrayList<>();

        for (ZonaPeligrosa z : zonasFiltradas) {
            long reportesRecientes = z.getReportes() != null 
                    ? z.getReportes().stream().filter(r -> r.getFechaCreacion() != null && r.getFechaCreacion().isAfter(limiteReciente)).count() 
                    : 0;

            Map<String, Object> zInfo = new HashMap<>();
            zInfo.put("zonaId", z.getId());
            zInfo.put("titulo", z.getTitulo() != null ? z.getTitulo() : "Incidente Sin Título");
            zInfo.put("reportesRecientes", reportesRecientes);
            zInfo.put("puntaje", z.getPuntaje() != null ? z.getPuntaje() : 0);
            zInfo.put("tasaCrecimiento", reportesRecientes > 0 ? "ALTA (+ " + reportesRecientes + " reportes/3 días)" : "ESTABLE");
            crecimientoZonas.add(zInfo);
        }

        // 2. Detección de Posibles Reportes Falsos e Inadecuados
        List<Map<String, Object>> posiblesFalsos = new ArrayList<>();
        List<Reporte> reportesSospechosos = reporteRepository.findSuspiciousReports();
        for (Reporte r : reportesSospechosos) {
            Map<String, Object> fInfo = new HashMap<>();
            fInfo.put("tipo", "REPORTE");
            fInfo.put("id", r.getId());
            fInfo.put("zonaId", r.getZona() != null ? r.getZona().getId() : null);
            fInfo.put("creador", r.getUsuario() != null ? r.getUsuario().getUsername() : "Anónimo");
            fInfo.put("contenido", r.getDescripcion());
            fInfo.put("razon", r.getJuicioJustificacion() != null ? r.getJuicioJustificacion() : "Contiene términos inadecuados");
            fInfo.put("clasificacion", r.getJuicioTipo() != null ? r.getJuicioTipo() : "FALSO");
            fInfo.put("evaluador", r.getEvaluador() != null ? r.getEvaluador() : "SISTEMA");
            posiblesFalsos.add(fInfo);
        }

        List<ComentarioZona> comentariosSospechosos = comentarioZonaRepository.findSuspiciousComments();
        for (ComentarioZona c : comentariosSospechosos) {
            Map<String, Object> fInfo = new HashMap<>();
            fInfo.put("tipo", "COMENTARIO");
            fInfo.put("id", c.getId());
            fInfo.put("zonaId", c.getZona() != null ? c.getZona().getId() : null);
            fInfo.put("creador", c.getUsuario() != null ? c.getUsuario().getUsername() : "Anónimo");
            fInfo.put("contenido", c.getContenido());
            fInfo.put("razon", c.getJuicioJustificacion() != null ? c.getJuicioJustificacion() : "Contiene términos inadecuados");
            fInfo.put("clasificacion", c.getJuicioTipo() != null ? c.getJuicioTipo() : "FALSO");
            fInfo.put("evaluador", c.getEvaluador() != null ? c.getEvaluador() : "SISTEMA");
            posiblesFalsos.add(fInfo);
        }

        // 3. Análisis de Horarios más Peligrosos
        List<Map<String, Object>> horarios = new ArrayList<>();
        List<Object[]> queryHoras = reporteRepository.countReportsByHoraRango();
        for (Object[] row : queryHoras) {
            Map<String, Object> hInfo = new HashMap<>();
            hInfo.put("rango", (String) row[0]);
            hInfo.put("cantidadIncidentes", ((Number) row[1]).intValue());
            horarios.add(hInfo);
        }
        horarios.sort((h1, h2) -> Integer.compare((Integer)h2.get("cantidadIncidentes"), (Integer)h1.get("cantidadIncidentes")));

        // 4. Incidentes Frecuentes
        List<Map<String, Object>> incidentes = new ArrayList<>();
        List<Object[]> queryIncidentes = reporteRepository.countReportsByTipoPeligro();
        for (Object[] row : queryIncidentes) {
            Map<String, Object> iInfo = new HashMap<>();
            iInfo.put("tipo", (String) row[0]);
            iInfo.put("cantidad", ((Number) row[1]).intValue());
            incidentes.add(iInfo);
        }
        incidentes.sort((i1, i2) -> Integer.compare((Integer)i2.get("cantidad"), (Integer)i1.get("cantidad")));

        return IaAnalysisResponse.builder()
                .crecimientoAceleradoZonas(crecimientoZonas)
                .posiblesReportesFalsos(posiblesFalsos)
                .horariosMasPeligrosos(horarios)
                .incidentesFrecuentes(incidentes)
                .build();
    }
}
