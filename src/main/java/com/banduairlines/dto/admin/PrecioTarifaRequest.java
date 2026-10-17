package com.banduairlines.dto.admin;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PrecioTarifaRequest(

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 0,
                message = "El precio admite hasta ocho dígitos y debe estar en pesos enteros")
        BigDecimal precio

) {
}