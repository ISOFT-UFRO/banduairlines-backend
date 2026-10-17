package com.banduairlines.dto.busqueda;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusquedaItinerarioRequest extends PasajerosBusquedaRequest {

    @NotBlank(message = "el origen es obligatorio")
    @Pattern(regexp = "[A-Za-z]{3}", message = "el origen debe ser un código IATA de tres letras")
    private String origen;

    @NotBlank(message = "el destino es obligatorio")
    @Pattern(regexp = "[A-Za-z]{3}", message = "el destino debe ser un código IATA de tres letras")
    private String destino;

    @NotNull(message = "la fecha de viaje es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @AssertTrue(message = "el origen y el destino deben ser distintos")
    public boolean isOrigenDestinoDistintos() {
        return origen == null || destino == null || !origen.equalsIgnoreCase(destino);
    }
}
