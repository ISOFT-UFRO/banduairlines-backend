package com.banduairlines.dto.admin;

import java.math.BigDecimal;

import com.banduairlines.model.enums.ClaseCabina;

public record TarifaResponse(
        Long id,
        Long vueloId,
        ClaseCabina claseCabina,
        Long packId,
        String nombrePack,
        Boolean packActivo,
        BigDecimal precio,
        String moneda
) {
}