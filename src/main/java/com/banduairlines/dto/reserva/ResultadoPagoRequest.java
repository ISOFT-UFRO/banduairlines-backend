package com.banduairlines.dto.reserva;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


//endpoint 13, funcionaiento de  POST /reservas/{reservaId}/resultado-pago. Lo envía PAGOS tras el retorno de Flow.

public record ResultadoPagoRequest(
        @NotBlank(message = "el pagoId es obligatorio")
        @Size(max = 100, message = "el pagoId no puede superar los 100 caracteres")
        String pagoId,

        @Size(max = 100, message = "la transaccionId no puede superar los 100 caracteres")
        String transaccionId,

        @NotNull(message = "el estado es obligatorio")
        Estado estado,

        @NotNull(message = "el montoCLP es obligatorio")
        @Positive(message = "el montoCLP debe ser mayor que cero")
        Long montoCLP,

        @NotNull(message = "la fecha es obligatoria")
        OffsetDateTime fecha,

        @Size(max = 255, message = "el motivoRechazo no puede superar los 255 caracteres")
        String motivoRechazo
) {

    public enum Estado {
        PAGADO,
        RECHAZADO
    }

    @AssertTrue(message = "la transaccionId es obligatoria cuando el pago es PAGADO")
    public boolean isTransaccionIdCoherente() {
        return estado != Estado.PAGADO || (transaccionId != null && !transaccionId.isBlank());
    }
}
