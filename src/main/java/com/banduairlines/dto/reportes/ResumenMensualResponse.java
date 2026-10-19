package com.banduairlines.dto.reportes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumen mensual histórico disponible para visualización")
public record ResumenMensualResponse(
        @Schema(example = "2026-09") String periodo,
        @Schema(example = "2026") Integer anio,
        @Schema(example = "9") Integer mes,
        @Schema(example = "Septiembre 2026") String nombrePeriodo) {
}
