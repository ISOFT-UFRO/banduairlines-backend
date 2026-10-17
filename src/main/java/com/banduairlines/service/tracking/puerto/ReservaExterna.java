package com.banduairlines.service.tracking.puerto;

import com.banduairlines.model.enums.EstadoReserva;

// Datos de Reserva que Tracking necesita para RF-04, sin exponer la entidad completa.
public record ReservaExterna(
        Long id,
        EstadoReserva estado
) {
}
