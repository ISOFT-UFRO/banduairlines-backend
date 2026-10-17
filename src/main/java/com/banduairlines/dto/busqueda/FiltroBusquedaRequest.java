package com.banduairlines.dto.busqueda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class FiltroBusquedaRequest extends BusquedaItinerarioRequest {

    @NotBlank
    @Pattern(regexp = "^(precio|horario_salida|duracion|escalas)$", message = "Criterio de ordenamiento inválido")
    private String ordenarPor = "precio";

    @NotBlank
    @Pattern(regexp = "^(asc|desc)$", message = "Orden debe ser asc o desc")
    private String orden = "asc";

    @Min(value = 0, message = "El precio mínimo debe ser mayor o igual a 0")
    private BigDecimal precioMinimo;

    @Min(value = 0, message = "El precio máximo debe ser mayor o igual a 0")
    private BigDecimal precioMaximo;

    private LocalTime horarioSalidaMinimo;

    private LocalTime horarioSalidaMaximo;

    @Min(value = 0, message = "La duración máxima no puede ser negativa")
    private Integer duracionMaximaMinutos;

    @Min(value = 0, message = "El máximo de escalas no puede ser negativo")
    @Max(value = 1, message = "Se admite como máximo una escala")
    private Integer maxEscalas;

    @AssertTrue(message = "El precio mínimo no puede superar el precio máximo")
    public boolean isRangoPreciosValido() {
        return precioMinimo == null || precioMaximo == null || precioMinimo.compareTo(precioMaximo) <= 0;
    }
}
