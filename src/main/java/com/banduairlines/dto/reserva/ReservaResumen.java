package com.banduairlines.dto.reserva;

import java.time.OffsetDateTime;
import java.util.List;

import com.banduairlines.model.enums.EstadoReserva;

//endpoin 15, para el funcionamiento del dey
public record ReservaResumen(
        Long reservaId,
        String codigoReserva,
        EstadoReserva estado,
        OffsetDateTime expiraEn,
        String pack,
        ReservaResponse.CantidadPasajeros cantidadPasajeros,
        List<ReservaResponse.Tramo> itinerario,
        TotalResponse total
) {
}
