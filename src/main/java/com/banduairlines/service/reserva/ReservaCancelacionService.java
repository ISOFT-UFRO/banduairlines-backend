package com.banduairlines.service.reserva;

import com.banduairlines.dto.reserva.ReservaCanceladaResponse;

/**
 * RESERVA: POST /reservas/{reservaId}/cancelacion (abandonar la compra; endpoint 3 de 1-6 y 12 de 7-12,
 * expuesto en ReservaPagoController) y la cancelación automática al vencer expiraEn.
 */
public interface ReservaCancelacionService {

    ReservaCanceladaResponse cancelar(Long reservaId, String compraToken);

    /** Cancela las compras abiertas cuyo bloqueo venció. Devuelve cuántas canceló. */
    int cancelarExpiradas();
}
