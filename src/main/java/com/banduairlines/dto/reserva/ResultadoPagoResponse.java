package com.banduairlines.dto.reserva;

import com.banduairlines.model.enums.EstadoReserva;

// endpoint 13
public record ResultadoPagoResponse(Long reservaId, EstadoReserva estado) {
}
