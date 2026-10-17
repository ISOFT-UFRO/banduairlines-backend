package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AvionRequestDto {
    @NotBlank(message = "La matrícula es obligatoria")
    @Size(max = 15)
    private String matricula;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 60)
    private String modelo;

    private List<AsientoRequestDto> asientos; // Mapa inicial de asientos opcional al crear
}
