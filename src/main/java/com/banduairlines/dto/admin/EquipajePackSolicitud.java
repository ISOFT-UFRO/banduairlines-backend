package com.banduairlines.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EquipajePackSolicitud(

    @NotNull(message = "Debe indicar si incluye equipaje de cabina")
    Boolean incluyeEquipajeCabina,

    @NotNull(message = "La cantidad de piezas de bodega es obligatoria")
    @Min(value = 0, message = "La cantidad de piezas no puede ser negativa")
    Integer piezasBodegaIncluidas,

    @NotNull(message = "El peso por pieza de bodega es obligatorio")
    @Min(value = 0, message = "El peso no puede ser negativo")
    Integer kgPorPiezaBodega

) {
}