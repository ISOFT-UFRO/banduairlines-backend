package com.banduairlines.service.busqueda;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MontosBusqueda {

    private MontosBusqueda() {
    }

    public static BigDecimal clp(BigDecimal monto) {
        return monto == null ? BigDecimal.ZERO : monto.setScale(0, RoundingMode.HALF_UP);
    }
}
