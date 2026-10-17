package com.banduairlines.dto.busqueda;

import java.util.List;

import com.banduairlines.model.enums.ClaseCabina;

public record SeleccionReservaResponse(
        List<Tramo> tramos,
        String pack,
        CantidadPasajeros cantidadPasajeros
) {
    public record Tramo(Long tramoId, ClaseCabina cabina) {
    }

    public record CantidadPasajeros(int adultos, int ninos, int bebes) {
    }
}
