package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReporteResponse {
    private Long id;
    private String creadorNombre;
    private String tipoPeligroNombre;
    private String descripcion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String fotoUrl;
    private LocalDateTime fechaCreacion;
}
