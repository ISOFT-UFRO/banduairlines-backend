package com.banduairlines.dto.admin;

import com.banduairlines.model.enums.ClaseCabina;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsientoRequest {
    @NotNull(message = "La fila es obligatoria")
    @Positive(message = "La fila debe ser mayor que cero")
    private Integer fila;

    @NotBlank(message = "La letra es obligatoria")
    @Size(max = 1)
    private String letra;

    @NotNull(message = "La clase de cabina es obligatoria")
    private ClaseCabina claseCabina;

    @NotNull(message = "Debe indicar si el asiento es preferente")
    private Boolean esPreferente;
}