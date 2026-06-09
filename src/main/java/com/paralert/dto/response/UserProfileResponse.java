package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String username;
    private String nombres;
    private String apellidos;
    private String dni;
    private String telefono;
    private String fotoPerfil;
    private Boolean verificado;
    private Integer nivelConfianza;
    private String estado;
    private Boolean alertasHabilitadas;
    private java.util.List<String> roles;
}
