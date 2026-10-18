package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IngresoDiarioProjection {
    LocalDate getFecha();
    String getMoneda();
    BigDecimal getIngresos();
    long getPagos();
}
