package com.banduairlines.dto.busqueda;

import com.banduairlines.model.enums.ClaseCabina;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SeleccionTramoCotizacionRequest(
        @NotNull @Positive Long vueloId,
        @NotNull @Positive Long tarifaId,
        @NotNull ClaseCabina claseCabina
) {
}
