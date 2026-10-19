package com.banduairlines.dto.reportes;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Punto de la serie comercial")
public record SerieVentasResponse(
        String periodo,
        Long totalVentas,
        BigDecimal ingresosTotales) {
}
