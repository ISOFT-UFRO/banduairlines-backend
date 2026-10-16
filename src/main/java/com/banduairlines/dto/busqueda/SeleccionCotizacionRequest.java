package com.banduairlines.dto.busqueda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeleccionCotizacionRequest extends PasajerosBusquedaRequest {

    // --- Identificadores ---

    @NotNull
    private Long vueloId;

    @NotNull
    private Long tarifaId;

    // --- Especificaciones ---

    @NotBlank
    private String claseCabina;

}
