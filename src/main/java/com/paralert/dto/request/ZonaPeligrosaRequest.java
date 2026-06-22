package com.paralert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ZonaPeligrosaRequest {

    private String titulo;

    private Long tipoPeligroId;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "La latitud es obligatoria")
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria")
    private BigDecimal longitud;

    private String nivelRiesgo;

    private Boolean forzarCreacion;

    private Long asociarZonaId;
}
