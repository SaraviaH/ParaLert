package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ZonaCalienteResponse {
    private Long zonaId;
    private String zonaNombre;
    private String nivelRiesgo;
    private long cantidadNotificaciones;
}
