package com.banduairlines.dto.admin;

import java.math.BigDecimal;

public record PackResponse(
        Long id,
        String nombre,
        String descripcion,
        Boolean incluyeEquipajeCabina,
        Integer piezasBodegaIncluidas,
        Integer kgPorPiezaBodega,
        Boolean incluyeSeleccionAsiento,
        Boolean incluyeEmbarquePrioritario,
        Boolean permiteCambios,
        BigDecimal multiplicadorMillas,
        Boolean activo
) {
}