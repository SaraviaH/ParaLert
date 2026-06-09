package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IaAnalysisResponse {
    private List<Map<String, Object>> crecimientoAceleradoZonas;
    private List<Map<String, Object>> posiblesReportesFalsos;
    private List<Map<String, Object>> horariosMasPeligrosos;
    private List<Map<String, Object>> incidentesFrecuentes;
}
