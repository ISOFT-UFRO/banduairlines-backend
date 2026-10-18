package com.banduairlines.dto.busqueda;

import java.time.LocalDate;
import java.util.List;

public record BusquedaItinerarioResponse(
        String origen,
        String destino,
        LocalDate fecha,
        ResumenPasajerosResponse pasajeros,
        List<ItinerarioVueloResponse> itinerarios
) {
}
