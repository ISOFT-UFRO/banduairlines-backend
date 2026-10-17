package com.banduairlines.dto.reportes;

import java.math.BigDecimal;

public interface IngresoPorMonedaProjection {
    String getMoneda();
    BigDecimal getIngresos();
}
