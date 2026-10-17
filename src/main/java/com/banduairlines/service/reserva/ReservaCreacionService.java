package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.CrearReservaRequest;
import com.banduairlines.dto.reserva.ReservaCreadaResponse;

/** Endpoint 1: POST /reservas */
public interface ReservaCreacionService {

    ReservaCreadaResponse crear(CrearReservaRequest request);
}
