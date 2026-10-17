package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.Nivel;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProgresoNivel(
        @Schema(description = "Nivel siguiente; null si el pasajero ya está en el nivel máximo", nullable = true)
        Nivel nivelSiguiente,
        @Schema(description = "Millas mínimas del nivel siguiente; null en el nivel máximo", nullable = true)
        Long millasNivelSiguiente,
        @Schema(description = "Millas que faltan para el nivel siguiente; 0 en el nivel máximo")
        long millasFaltantes,
        @Schema(description = "Avance dentro del nivel actual, de 0 a 100")
        int porcentaje
) {
}
