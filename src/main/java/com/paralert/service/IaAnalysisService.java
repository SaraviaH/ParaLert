package com.paralert.service;

import com.paralert.dto.response.IaAnalysisResponse;
import com.paralert.entity.ComentarioZona;
import com.paralert.entity.Reporte;
import com.paralert.entity.ZonaPeligrosa;
import com.paralert.repository.ComentarioZonaRepository;
import com.paralert.repository.ReporteRepository;
import com.paralert.repository.ZonaPeligrosaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IaAnalysisService {

    private final ZonaPeligrosaRepository zonaPeligrosaRepository;
    private final ReporteRepository reporteRepository;
    private final ComentarioZonaRepository comentarioZonaRepository;

    @Transactional(readOnly = true)
    public IaAnalysisResponse generarReporteIa() {
        log.info("Iniciando análisis de patrones mediante IA (versión optimizada en base de datos)...");

        LocalDateTime limiteReciente = LocalDateTime.now().minusDays(3);

        // 1. Detección de Zonas con Crecimiento Acelerado (solo cargamos las que cumplen criterios)
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

        // 2. Detección de Posibles Reportes Falsos (consultando solo coincidencias de base de datos)
        List<Map<String, Object>> posiblesFalsos = new ArrayList<>();
        List<Reporte> reportesSospechosos = reporteRepository.findSuspiciousReports();
        for (Reporte r : reportesSospechosos) {
            Map<String, Object> fInfo = new HashMap<>();
            fInfo.put("tipo", "REPORTE");
            fInfo.put("id", r.getId());
            fInfo.put("zonaId", r.getZona() != null ? r.getZona().getId() : null);
            fInfo.put("creador", r.getUsuario() != null ? r.getUsuario().getUsername() : "Anónimo");
            fInfo.put("contenido", r.getDescripcion());
            fInfo.put("razon", "Uso de palabras clave sospechosas (broma, fake, etc.)");
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
            fInfo.put("razon", "Reporte vecinal cuestiona veracidad o usa palabras sospechosas");
            posiblesFalsos.add(fInfo);
        }

        // 3. Análisis de Horarios más Peligrosos (agrupación nativa en SQL)
        List<Map<String, Object>> horarios = new ArrayList<>();
        List<Object[]> queryHoras = reporteRepository.countReportsByHoraRango();
        for (Object[] row : queryHoras) {
            Map<String, Object> hInfo = new HashMap<>();
            hInfo.put("rango", (String) row[0]);
            hInfo.put("cantidadIncidentes", ((Number) row[1]).intValue());
            horarios.add(hInfo);
        }
        horarios.sort((h1, h2) -> Integer.compare((Integer)h2.get("cantidadIncidentes"), (Integer)h1.get("cantidadIncidentes")));

        // 4. Incidentes Frecuentes (agrupación nativa en SQL)
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
