package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ZonaPeligrosaResponse {
    private Long id;
    private String titulo;
    private String descripcion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String nivelRiesgo;
    private Integer puntaje;
    private Integer radio;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimaActividad;
    private String creadorNombre;
    private Double calificacionPromedio;
    private Integer totalComentarios;
    private Integer totalConfirmaciones;
    private String fotoUrl;
    private Long tipoPeligroId;
    private Integer creadorNivelConfianza;
    private List<ReporteResponse> reportes;
    private Boolean confirmadoPorUsuario;
}
