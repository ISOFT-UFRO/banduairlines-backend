package com.banduairlines.dto.cuenta;

import io.swagger.v3.oas.annotations.media.Schema;

public record RespuestaCanje(
        @Schema(description = "Identificador del canje (id del movimiento CANJE)")
        Long idCanje,
        @Schema(description = "Saldo de puntos antes del canje")
        long saldoAnterior,
        @Schema(description = "Puntos descontados por el canje")
        long puntosDescontados,
        @Schema(description = "Saldo de puntos después del canje")
        long saldoActual,
        @Schema(description = "Movimiento CANJE registrado en el historial")
        RespuestaMovimientoPuntos movimiento
) {
}
