package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import com.banduairlines.model.enums.EstadoPago;

public record EstadoPagoMetricaResponse(
        EstadoPago estado,
        Long cantidad,
        BigDecimal monto) {
}
