package com.banduairlines.dto.busqueda;

import java.util.List;

public record TarifasBusquedaResponse(
        List<Long> vueloIds,
        ResumenPasajerosResponse pasajeros,
        List<TarifaItinerarioResponse> tarifas
) {
}
