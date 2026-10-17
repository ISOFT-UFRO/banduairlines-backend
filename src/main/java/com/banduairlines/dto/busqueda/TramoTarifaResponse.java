package com.banduairlines.dto.busqueda;

import java.math.BigDecimal;

import com.banduairlines.model.enums.ClaseCabina;

public record TramoTarifaResponse(
        Long vueloId,
        String codigoVuelo,
        Long tarifaId,
        ClaseCabina claseCabina,
        BigDecimal precioPorPasajero
) {
}
