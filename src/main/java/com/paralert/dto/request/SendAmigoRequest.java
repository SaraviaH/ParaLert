package com.paralert.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendAmigoRequest {
    @NotBlank(message = "El identificador es obligatorio")
    private String identificador;
}
