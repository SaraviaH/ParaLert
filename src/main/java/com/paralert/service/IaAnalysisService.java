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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IaAnalysisService {

    private final ZonaPeligrosaRepository zonaPeligrosaRepository;
    private final ReporteRepository reporteRepository;
    private final ComentarioZonaRepository comentarioZonaRepository;

    @Transactional(readOnly = true)
    public IaAnalysisResponse generarReporteIa() {
        log.info("Iniciando análisis de patrones mediante IA...");

        List<ZonaPeligrosa> zonas = zonaPeligrosaRepository.findAll();
        List<Reporte> reportes = reporteRepository.findAll();
        List<ComentarioZona> comentarios = comentarioZonaRepository.findAll();

        // 1. Detección de Zonas con Crecimiento Acelerado
        // Heurística: Zonas activas con alto puntaje acumulado en poco tiempo (ej: más de 2 reportes o SOS)
        List<Map<String, Object>> crecimientoZonas = new ArrayList<>();
        LocalDateTime limiteReciente = LocalDateTime.now().minusDays(3);

        for (ZonaPeligrosa z : zonas) {
            long reportesRecientes = z.getReportes() != null 
                    ? z.getReportes().stream().filter(r -> r.getFechaCreacion() != null && r.getFechaCreacion().isAfter(limiteReciente)).count() 
                    : 0;

            if (reportesRecientes >= 2 || (z.getPuntaje() != null && z.getPuntaje() > 100)) {
                Map<String, Object> zInfo = new HashMap<>();
                zInfo.put("zonaId", z.getId());
                zInfo.put("titulo", z.getTitulo() != null ? z.getTitulo() : "Incidente Sin Título");
                zInfo.put("reportesRecientes", reportesRecientes);
                zInfo.put("puntaje", z.getPuntaje() != null ? z.getPuntaje() : 0);
                zInfo.put("tasaCrecimiento", reportesRecientes > 0 ? "ALTA (+ " + reportesRecientes + " reportes/3 días)" : "ESTABLE");
                crecimientoZonas.add(zInfo);
            }
        }

        // 2. Detección de Posibles Reportes Falsos
        // Heurística: Reportes o comentarios que incluyan palabras sospechosas o contradicciones
        List<Map<String, Object>> posiblesFalsos = new ArrayList<>();
        List<String> keywordsSospechosas = Arrays.asList("falso", "mentira", "broma", "fake", "prueba", "test", "inventado", "ninguno");

        for (Reporte r : reportes) {
            if (r.getDescripcion() == null) continue;
            String desc = r.getDescripcion().toLowerCase();
            boolean sospechoso = keywordsSospechosas.stream().anyMatch(desc::contains);

            if (sospechoso) {
                Map<String, Object> fInfo = new HashMap<>();
                fInfo.put("tipo", "REPORTE");
                fInfo.put("id", r.getId());
                fInfo.put("zonaId", r.getZona() != null ? r.getZona().getId() : null);
                fInfo.put("creador", r.getUsuario() != null ? r.getUsuario().getUsername() : "Anónimo");
                fInfo.put("contenido", r.getDescripcion());
                fInfo.put("razon", "Uso de palabras clave sospechosas (broma, fake, etc.)");
                posiblesFalsos.add(fInfo);
            }
        }

        for (ComentarioZona c : comentarios) {
            if (c.getContenido() == null) continue;
            String cont = c.getContenido().toLowerCase();
            boolean sospechoso = keywordsSospechosas.stream().anyMatch(cont::contains);

            if (sospechoso) {
                Map<String, Object> fInfo = new HashMap<>();
                fInfo.put("tipo", "COMENTARIO");
                fInfo.put("id", c.getId());
                fInfo.put("zonaId", c.getZona() != null ? c.getZona().getId() : null);
                fInfo.put("creador", c.getUsuario() != null ? c.getUsuario().getUsername() : "Anónimo");
                fInfo.put("contenido", c.getContenido());
                fInfo.put("razon", "Reporte vecinal cuestiona veracidad o usa palabras sospechosas");
                posiblesFalsos.add(fInfo);
            }
        }

        // 3. Análisis de Horarios más Peligrosos
        // Heurística: Agrupar horas de creación de reportes
        Map<String, Integer> horasMap = new HashMap<>();
        horasMap.put("Madrugada (00:00 - 06:00)", 0);
        horasMap.put("Mañana (06:00 - 12:00)", 0);
        horasMap.put("Tarde (12:00 - 18:00)", 0);
        horasMap.put("Noche (18:00 - 00:00)", 0);

        for (Reporte r : reportes) {
            if (r.getFechaCreacion() == null) continue;
            int hora = r.getFechaCreacion().getHour();
            if (hora >= 0 && hora < 6) {
                horasMap.put("Madrugada (00:00 - 06:00)", horasMap.get("Madrugada (00:00 - 06:00)") + 1);
            } else if (hora >= 6 && hora < 12) {
                horasMap.put("Mañana (06:00 - 12:00)", horasMap.get("Mañana (06:00 - 12:00)") + 1);
            } else if (hora >= 12 && hora < 18) {
                horasMap.put("Tarde (12:00 - 18:00)", horasMap.get("Tarde (12:00 - 18:00)") + 1);
            } else {
                horasMap.put("Noche (18:00 - 00:00)", horasMap.get("Noche (18:00 - 00:00)") + 1);
            }
        }

        List<Map<String, Object>> horarios = new ArrayList<>();
        horasMap.forEach((k, v) -> {
            Map<String, Object> hInfo = new HashMap<>();
            hInfo.put("rango", k);
            hInfo.put("cantidadIncidentes", v);
            horarios.add(hInfo);
        });
        horarios.sort((h1, h2) -> Integer.compare((Integer)h2.get("cantidadIncidentes"), (Integer)h1.get("cantidadIncidentes")));

        // 4. Incidentes Frecuentes
        Map<String, Integer> incidentesMap = new HashMap<>();
        for (Reporte r : reportes) {
            String tipoNombre = r.getTipoPeligro() != null ? r.getTipoPeligro().getNombre() : "Otro";
            incidentesMap.put(tipoNombre, incidentesMap.getOrDefault(tipoNombre, 0) + 1);
        }

        List<Map<String, Object>> incidentes = new ArrayList<>();
        incidentesMap.forEach((k, v) -> {
            Map<String, Object> iInfo = new HashMap<>();
            iInfo.put("tipo", k);
            iInfo.put("cantidad", v);
            incidentes.add(iInfo);
        });
        incidentes.sort((i1, i2) -> Integer.compare((Integer)i2.get("cantidad"), (Integer)i1.get("cantidad")));

        return IaAnalysisResponse.builder()
                .crecimientoAceleradoZonas(crecimientoZonas)
                .posiblesReportesFalsos(posiblesFalsos)
                .horariosMasPeligrosos(horarios)
                .incidentesFrecuentes(incidentes)
                .build();
    }
}
