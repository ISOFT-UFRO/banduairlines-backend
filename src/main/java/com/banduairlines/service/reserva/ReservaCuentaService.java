package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaResponse;

// endpoint 16, vincula una compra de invitado a la cuenta del pasajero autenticado
public interface ReservaCuentaService {

    ReservaResponse asociar(Long reservaId, String compraToken);
}