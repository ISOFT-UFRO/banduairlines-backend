package com.banduairlines.dto.admin;

import java.math.BigDecimal;

import com.banduairlines.model.enums.ClaseCabina;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record TarifaRequest(

        @NotNull(message = "La clase de cabina es obligatoria")
        ClaseCabina claseCabina,

        @NotNull(message = "El ID del pack es obligatorio")
        @Positive(message = "El ID del pack debe ser positivo")
        Long packId,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 0,
                message = "El precio admite hasta ocho dígitos y debe estar en pesos enteros")
        BigDecimal precio

) {
}