package com.banduairlines.dto.busqueda;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TarifasBusquedaRequest extends PasajerosBusquedaRequest {

    @NotEmpty(message = "debe indicar los vuelos del itinerario")
    @Size(max = 2, message = "se admite como máximo una conexión")
    private List<@NotNull @Positive(message = "el ID del vuelo debe ser positivo") Long> vueloIds;

    @AssertTrue(message = "un vuelo no puede repetirse en el itinerario")
    public boolean isVuelosSinRepetir() {
        return vueloIds == null || vueloIds.stream().distinct().count() == vueloIds.size();
    }
}
