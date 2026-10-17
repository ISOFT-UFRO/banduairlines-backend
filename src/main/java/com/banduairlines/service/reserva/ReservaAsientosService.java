package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.AsientosAsignadosResponse;
import com.banduairlines.dto.reserva.MapaAsientosResponse;
import com.banduairlines.dto.reserva.SeleccionarAsientosRequest;

/**
 * Endpoint 6: GET /reservas/{reservaId}/tramos/{tramoId}/asientos
 * y su complemento 6b: PUT /reservas/{reservaId}/asientos, que elige los asientos y los bloquea hasta expiraEn (RF-01).
 */
public interface ReservaAsientosService {

    MapaAsientosResponse obtenerMapa(Long reservaId, Long tramoId, String compraToken);

    AsientosAsignadosResponse seleccionar(Long reservaId, String compraToken, SeleccionarAsientosRequest request);
}
