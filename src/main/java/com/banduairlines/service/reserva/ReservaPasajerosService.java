package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.PasajerosRegistradosResponse;
import com.banduairlines.dto.reserva.RegistrarPasajerosRequest;

/** Endpoint 4: PUT /reservas/{reservaId}/pasajeros */
public interface ReservaPasajerosService {

    PasajerosRegistradosResponse registrar(Long reservaId, String compraToken, RegistrarPasajerosRequest request);
}
