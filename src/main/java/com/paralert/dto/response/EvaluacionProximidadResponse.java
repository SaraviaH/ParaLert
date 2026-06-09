package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EvaluacionProximidadResponse {
    private String nivelRiesgo; // NINGUNO, BAJO, MEDIO, ALTO
    private Double distanciaMetros;
    private Long zonaId;
    private String zonaNombre;
    private String mensaje;
    private Boolean notificadoContactos;
}
