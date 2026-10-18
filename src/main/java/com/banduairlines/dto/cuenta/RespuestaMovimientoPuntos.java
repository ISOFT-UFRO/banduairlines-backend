package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record RespuestaMovimientoPuntos(
        @Schema(description = "Identificador del movimiento; en un canje es el idCanje")
        Long idMovimiento,
        @Schema(description = "ACUMULACION o CANJE")
        TipoMovimiento tipo,
        @Schema(description = "Puntos del movimiento: positivo en una acumulación y negativo en un canje", example = "-500")
        long cantidadPuntos,
        Instant fecha,
        @Schema(description = "En un canje incluye el nombre que tenía el beneficio al canjearlo")
        String descripcion,
        @Schema(description = "Código de la reserva asociada a una acumulación", nullable = true)
        String codigoPNR,
        @Schema(description = "Beneficio canjeado; null en las acumulaciones", nullable = true)
        Long idBeneficio
) {
}
