package com.paralert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String nombres;
    private String username;
    private Integer nivelConfianza;
    private Boolean verificado;
    private Boolean alertasHabilitadas;
    private java.util.List<String> roles;
}
