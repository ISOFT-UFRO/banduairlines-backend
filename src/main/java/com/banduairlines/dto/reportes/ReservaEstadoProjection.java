package com.banduairlines.dto.reportes;

import com.banduairlines.model.enums.EstadoReserva;

public interface ReservaEstadoProjection {
    EstadoReserva getEstado();
    long getCantidad();
}
