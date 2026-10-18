package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import com.banduairlines.model.enums.EstadoReserva;

public record EstadoReservaMetricaResponse(
        EstadoReserva estado,
        Long cantidad,
        BigDecimal monto) {
}
