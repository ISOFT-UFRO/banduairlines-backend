package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaResponse;

/** Endpoint 3: POST /reservas/{reservaId}/cancelacion */
public interface ReservaCancelacionService {

    ReservaResponse cancelar(Long reservaId, String compraToken);
}
