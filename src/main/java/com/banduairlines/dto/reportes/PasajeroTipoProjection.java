package com.banduairlines.dto.reportes;

import com.banduairlines.model.enums.TipoPasajero;

public interface PasajeroTipoProjection {
    TipoPasajero getTipo();
    long getCantidad();
}
