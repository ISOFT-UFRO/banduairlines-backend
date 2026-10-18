package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReporteResponse(
        LocalDate fecha,
        String moneda,
        long totalVentas,
        BigDecimal totalIngresos,
        long reservasPendientes,
        long asientosAsignados,
        long cantidadAdultos,
        long cantidadNinios,
        long cantidadBebes
) {
}