package com.banduairlines.dto.busqueda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapacidadCabinaItemDTO {

    // --- Atributos ---

    @NotNull(message = "La clase de cabina es obligatoria")
    private String claseCabina;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 0, message = "La capacidad no puede ser negativa")
    private Integer capacidad;
}