package com.banduairlines.dto.reportes;

import java.math.BigDecimal;

public interface VentaExtraProjection {
    String getNombre();
    String getTipo();
    String getMoneda();
    BigDecimal getIngresos();
    long getUnidades();
}
