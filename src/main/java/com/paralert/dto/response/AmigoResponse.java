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
public class AmigoResponse {
    private Long id;
    private Long amigoId;
    private String email;
    private String username;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String fotoPerfil;
    private Boolean verificado;
    private Integer nivelConfianza;
    private String prioridad;
    private LocalDateTime fechaRegistro;
}
