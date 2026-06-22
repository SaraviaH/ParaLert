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
public class SolicitudAmistadResponse {
    private Long id;
    private Long emisorId;
    private String emisorNombres;
    private String emisorUsername;
    private String emisorEmail;
    private String emisorFotoPerfil;
    private Long receptorId;
    private String receptorNombres;
    private String receptorUsername;
    private String receptorEmail;
    private String receptorFotoPerfil;
    private String estado;
    private LocalDateTime fechaEnvio;
}
