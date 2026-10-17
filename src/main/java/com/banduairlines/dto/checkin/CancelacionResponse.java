package com.banduairlines.dto.checkin;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CancelacionResponse(
        @JsonProperty("codigo_reserva") String codigoReserva,
        @JsonProperty("estado_reserva") String estadoReserva,
        @JsonProperty("fecha_cancelacion") OffsetDateTime fechaCancelacion,
        boolean reembolsable,
        @JsonProperty("monto_reembolsable") long montoReembolsable
) {
}
