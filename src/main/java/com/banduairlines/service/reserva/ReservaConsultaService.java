package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaResponse;

/** Endpoint 2: GET /reservas/{reservaId} */
public interface ReservaConsultaService {

    ReservaResponse obtener(Long reservaId, String compraToken);
}
