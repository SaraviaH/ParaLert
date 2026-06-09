package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ComentarioZonaResponse {
    private Long id;
    private String contenido;
    private Integer calificacion;
    private LocalDateTime fechaCreacion;
    private String creadorNombre;
    private String creadorFotoPerfil;
    private String fotoUrl;
}
