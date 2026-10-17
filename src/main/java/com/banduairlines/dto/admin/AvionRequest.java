package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AvionRequest {
    @NotBlank(message = "La matrícula es obligatoria")
    @Size(max = 15)
    private String matricula;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 60)
    private String modelo;
    @Valid    private List<@NotNull(message = "La lista no puede contener asientos nulos")
        AsientoRequest> asientos;
}
