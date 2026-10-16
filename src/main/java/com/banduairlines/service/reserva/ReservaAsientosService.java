package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.MapaAsientosResponse;

/** Endpoint 6: GET /reservas/{reservaId}/tramos/{tramoId}/asientos */
public interface ReservaAsientosService {

    MapaAsientosResponse obtenerMapa(Long reservaId, Long tramoId, String compraToken);
}
