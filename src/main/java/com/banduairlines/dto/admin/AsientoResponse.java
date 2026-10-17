package com.banduairlines.dto.admin;

import com.banduairlines.model.enums.ClaseCabina;

public record AsientoResponse(
        Long id,
        Long aeronaveId,
        Integer fila,
        String letra,
        ClaseCabina claseCabina,
        Boolean esPreferente
) {
}