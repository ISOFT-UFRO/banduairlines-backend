package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.Nivel;
import io.swagger.v3.oas.annotations.media.Schema;

public record RespuestaBeneficio(
        Long idBeneficio,
        String nombre,
        String descripcion,
        Nivel nivelRequerido,
        Integer costoPuntos,
        @Schema(description = "true si el pasajero puede canjearlo ahora (motivoBloqueo es null)")
        boolean disponible,
        @Schema(description = "Razón del bloqueo; null cuando el beneficio se puede canjear", nullable = true)
        MotivoBloqueo motivoBloqueo
) {
}