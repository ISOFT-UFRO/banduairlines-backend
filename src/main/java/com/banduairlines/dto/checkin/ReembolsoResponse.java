package com.banduairlines.dto.checkin;


import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReembolsoResponse(
        @JsonProperty("id_reembolso") Long idReembolso,
        @JsonProperty("codigo_reserva") String codigoReserva,
        String tipo,
        String estado,
        long monto,
        @JsonProperty("fecha_solicitud") OffsetDateTime fechaSolicitud,
        Cupon cupon
) {
    public record Cupon(
            String codigo,
            long monto,
            @JsonProperty("vigente_hasta") OffsetDateTime vigenteHasta
    ) {
    }
}
