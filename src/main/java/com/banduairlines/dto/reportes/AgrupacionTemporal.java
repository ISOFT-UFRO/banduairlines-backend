package com.banduairlines.dto.reportes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agrupación temporal de la serie de ventas e ingresos")
public enum AgrupacionTemporal {
    DIARIA,
    SEMANAL,
    MENSUAL,
    ANUAL
}
