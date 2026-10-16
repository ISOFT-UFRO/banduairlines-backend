package com.banduairlines.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IngresoDiarioResponse(
        LocalDate fecha,
        BigDecimal totalIngresos
) {
}