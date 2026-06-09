package com.paralert.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 150)
    private String nombres;

    @Size(max = 150)
    private String apellidos;

    @Size(max = 20)
    private String telefono;
}
