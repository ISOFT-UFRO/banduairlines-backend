package com.banduairlines.dto.reportes;

import com.banduairlines.model.enums.TipoPasajero;

public record TipoPasajeroMetricaResponse(
        TipoPasajero tipo,
        Long cantidad,
        Double proporcion) {
}
