package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ContactoRequest;
import com.banduairlines.dto.reserva.ContactoResponse;

/** Endpoint 5: PUT /reservas/{reservaId}/contacto */
public interface ReservaContactoService {

    ContactoResponse registrar(Long reservaId, String compraToken, ContactoRequest request);
}
