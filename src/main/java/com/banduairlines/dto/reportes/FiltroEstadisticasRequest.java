package com.banduairlines.dto.reportes;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.banduairlines.model.enums.ClaseCabina;
import com.banduairlines.model.enums.EstadoPago;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.model.enums.TipoPasajero;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Schema(description = "Filtros opcionales para estadísticas comerciales")
public record FiltroEstadisticasRequest(

        @Schema(example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @Schema(example = "2026-10-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin,

        AgrupacionTemporal agrupacion,

        @Positive(message = "rutaId debe ser mayor que cero")
        Long rutaId,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "origenIata debe contener exactamente 3 letras")
        String origenIata,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "destinoIata debe contener exactamente 3 letras")
        String destinoIata,

        EstadoReserva estadoReserva,
        EstadoPago estadoPago,
        TipoPasajero tipoPasajero,

        @Positive(message = "packTarifarioId debe ser mayor que cero")
        Long packTarifarioId,

        ClaseCabina claseCabina) {

    public FiltroEstadisticasRequest normalizado() {
        return new FiltroEstadisticasRequest(
                fechaInicio,
                fechaFin,
                agrupacion,
                rutaId,
                normalizarIata(origenIata),
                normalizarIata(destinoIata),
                estadoReserva,
                estadoPago,
                tipoPasajero,
                packTarifarioId,
                claseCabina);
    }

    private static String normalizarIata(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toUpperCase();
    }
}
