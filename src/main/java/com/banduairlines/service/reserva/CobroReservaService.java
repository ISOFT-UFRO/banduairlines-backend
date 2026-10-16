package com.banduairlines.service.reserva;

import com.banduairlines.model.Reserva;

/**
 * Lo que RESERVA necesita de PAGOS para iniciar el cobro (contrato de RESERVA, sección 6:
 * "POST /pagos con { reservaId, montoCLP, correo } → { pagoId, urlPago }").
 * PAGOS debe entregar la implementación real; mientras tanto existe CobroReservaServiceProvisional.
 * Si PAGOS o Flow fallan, la implementación lanza ServicioExternoException.
 */
public interface CobroReservaService {

    CobroIniciado iniciarCobro(Reserva reserva, long montoCLP, String correo);

    /**
     * true si PAGOS ya registró al menos un cobro para la reserva, en cualquier estado
     * (pendiente, pagado o rechazado). RESERVA lo usa para saber si la compra sigue en curso:
     * una reserva PENDIENTE_PAGO sin cobros todavía se puede modificar.
     */
    boolean tieneCobroIniciado(Long reservaId);

    record CobroIniciado(String pagoId, String urlPago) {
    }
}
