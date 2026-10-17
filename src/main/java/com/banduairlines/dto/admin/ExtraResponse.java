package com.banduairlines.dto.admin;

import java.math.BigDecimal;

import com.banduairlines.model.enums.TipoExtra;

public record ExtraResponse(
        Long id,
        TipoExtra tipo,
        String nombre,
        Integer pesoKg,
        BigDecimal precio,
        Boolean activo
) {
}
