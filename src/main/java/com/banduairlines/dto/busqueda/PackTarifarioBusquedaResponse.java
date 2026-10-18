package com.banduairlines.dto.busqueda;

import java.math.BigDecimal;

public record PackTarifarioBusquedaResponse(
        Long id,
        String nombre,
        String descripcion,
        Boolean incluyeEquipajeCabina,
        Integer piezasBodegaIncluidas,
        Integer kgPorPiezaBodega,
        Boolean incluyeSeleccionAsiento,
        Boolean incluyeEmbarquePrioritario,
        Boolean permiteCambios,
        BigDecimal multiplicadorMillas
) {
}
