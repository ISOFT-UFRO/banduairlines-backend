package com.banduairlines.dto.admin;

import jakarta.validation.constraints.NotNull;

public record EstadoRutaRequest(
        @NotNull(message = "El estado de activación es obligatorio")
        Boolean activa
) {
}