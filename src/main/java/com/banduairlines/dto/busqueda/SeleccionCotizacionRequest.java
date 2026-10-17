package com.banduairlines.dto.busqueda;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeleccionCotizacionRequest extends PasajerosBusquedaRequest {

    @Positive
    private Long vueloId;

    @Positive
    private Long tarifaId;

    @Pattern(regexp = "ECONOMY|PREMIUM_ECONOMY|BUSINESS", message = "La clase de cabina no es válida")
    private String claseCabina;

    @Size(min = 1, max = 2, message = "Debe seleccionar uno o dos tramos")
    private List<@NotNull @Valid SeleccionTramoCotizacionRequest> tramos;

    @AssertTrue(message = "Indique vuelo, tarifa y cabina, o una lista de tramos, sin mezclar ambos formatos")
    public boolean isSeleccionCompleta() {
        if (tramos != null) {
            return vueloId == null && tarifaId == null && claseCabina == null;
        }
        return vueloId != null && tarifaId != null && claseCabina != null;
    }

    @AssertTrue(message = "Un vuelo no puede repetirse en la selección")
    public boolean isTramosSinRepetir() {
        if (tramos == null) {
            return true;
        }
        var ids = tramos.stream().filter(Objects::nonNull)
                .map(SeleccionTramoCotizacionRequest::vueloId).filter(Objects::nonNull).toList();
        return ids.size() == ids.stream().distinct().count();
    }
}
