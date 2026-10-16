package com.banduairlines.dto.cuenta;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record RespuestaCanje(
        @Schema(description = "Identificador del canje (id del movimiento CANJE)")
        Long idCanje,
        Long idBeneficio,
        String nombreBeneficio,
        long puntosCanjeados,
        @Schema(description = "Saldo de puntos después del canje")
        long saldoPuntos,
        Instant fecha
) {
}
