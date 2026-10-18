package com.banduairlines.dto.reportes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ocupación agregada de asientos")
public record OcupacionAsientosResponse(
        Long asientosOcupados,
        Long capacidadAsientos,
        Double porcentajeOcupacion) {
}
