package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserResponse {
    private Long id;
    private String email;
    private String username;
    private String nombres;
    private String apellidos;
    private String dni;
    private String telefono;
    private String estado;
    private Integer nivelConfianza;
    private Boolean verificado;
    private LocalDateTime fechaRegistro;
    private List<String> roles;
}
