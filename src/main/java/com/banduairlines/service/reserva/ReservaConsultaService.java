package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaResponse;
import com.banduairlines.model.Reserva;

/** Endpoint 2: GET /reservas/{reservaId}. armarRespuesta también la usan los endpoints 13 a 16. */
public interface ReservaConsultaService {

    ReservaResponse obtener(Long reservaId, String compraToken);

    /** Arma la reserva completa sin revisar acceso: quien llama ya autorizó la reserva. */
    ReservaResponse armarRespuesta(Reserva reserva);
}
