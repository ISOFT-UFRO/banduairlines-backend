package com.banduairlines.dto.busqueda;

import java.math.BigDecimal;
import java.util.List;

public record TarifaItinerarioResponse(
        PackTarifarioBusquedaResponse pack,
        String moneda,
        BigDecimal precioPorPasajero,
        BigDecimal precioTotalTarifas,
        List<TramoTarifaResponse> tramos
) {
}
