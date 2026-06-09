package com.paralert.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendContactRequest {

    // Puede ser email o username
    @NotBlank(message = "El identificador es obligatorio")
    private String identificador;
}
