package com.banduairlines.dto.admin;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PackRequest(

        @NotBlank(message = "El nombre del pack es obligatorio")
        @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion,

        @NotNull(message = "Debe indicar si incluye selección de asiento")
        Boolean incluyeSeleccionAsiento,

        @NotNull(message = "Debe indicar si incluye embarque prioritario")
        Boolean incluyeEmbarquePrioritario,

        @NotNull(message = "Debe indicar si permite cambios")
        Boolean permiteCambios,

        @NotNull(message = "El multiplicador de millas es obligatorio")
        @PositiveOrZero(message = "El multiplicador no puede ser negativo")
        @Digits(integer = 1, fraction = 2,
                message = "El multiplicador admite un entero y hasta dos decimales")
        BigDecimal multiplicadorMillas

) {
}