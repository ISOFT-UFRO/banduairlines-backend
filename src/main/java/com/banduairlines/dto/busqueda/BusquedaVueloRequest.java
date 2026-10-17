package com.banduairlines.dto.busqueda;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BusquedaVueloRequest(
        @NotNull @Positive Long origenId,
        @NotNull @Positive Long destinoId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
}
