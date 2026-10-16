package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SolicitudCanje(
        @NotNull(message = "el idBeneficio es obligatorio")
        @Positive(message = "el idBeneficio debe ser positivo")
        Long idBeneficio
) {
}
