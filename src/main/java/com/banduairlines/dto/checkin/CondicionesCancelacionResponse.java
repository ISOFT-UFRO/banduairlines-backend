package com.banduairlines.dto.checkin;

import java.time.OffsetDateTime;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonProperty;

public record CondicionesCancelacionResponse(

        @JsonProperty("codigo_reserva") String codigoReserva,
        @JsonProperty("estado_reserva") String estadoReserva,
        boolean cancelable,
        boolean reembolsable,
        MotivoRestriccion motivo,
        @JsonProperty("fecha_limite_cancelacion") OffsetDateTime fechaLimiteCancelacion,
        @JsonProperty("monto_pagado") long montoPagado,
        @JsonProperty("monto_reembolsable") long montoReembolsable,
        List<Tramo> tramos,
        List<Pasajero> pasajeros
) {

    public record Tramo(
            @JsonProperty("codigo_vuelo") String codigoVuelo,
            @JsonProperty("codigo_origen") String codigoOrigen,
            @JsonProperty("codigo_destino") String codigoDestino,
            @JsonProperty("salida_programada") OffsetDateTime salidaProgramada,
            @JsonProperty("estado_vuelo") String estadoVuelo,
            @JsonProperty("clase_cabina") String claseCabina,
            String tarifa,
            @JsonProperty("tarifa_reembolsable") boolean tarifaReembolsable
    ) {
    }
    public record Pasajero(
            String nombres,
            String apellidos,
            String tipo,
            boolean checkin
    ){

    }
}
