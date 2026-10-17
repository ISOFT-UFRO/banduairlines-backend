package com.banduairlines.dto.busqueda;

public record ResumenPasajerosResponse(
        int cantidadAdultos,
        int cantidadNinos,
        int cantidadBebes,
        long totalPasajeros,
        long asientosNecesarios
) {
}
