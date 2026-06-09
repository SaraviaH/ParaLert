package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminStatsResponse {
    private long totalUsuarios;
    private long totalZonas;
    private long totalComentarios;
    private long totalSosActivos;
    private long totalTiposPeligro;
    private long totalAfectados;
    private Map<String, Long> distribucionRiesgo;
    private Map<String, Long> distribucionCategoria;
    private List<ZonaCalienteResponse> zonasCalientes;
}
