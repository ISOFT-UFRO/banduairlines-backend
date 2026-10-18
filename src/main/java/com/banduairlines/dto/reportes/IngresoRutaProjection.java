package com.banduairlines.dto.reportes;

import java.math.BigDecimal;

public interface IngresoRutaProjection {
    String getOrigen();
    String getDestino();
    String getMoneda();
    BigDecimal getIngresos();
    long getBoletos();
}
