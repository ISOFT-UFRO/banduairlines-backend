package com.banduairlines.dto.busqueda;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasajerosBusquedaRequest {

    @NotNull(message = "la cantidad de adultos es obligatoria")
    @Min(value = 0, message = "la cantidad de adultos no puede ser negativa")
    private Integer cantidadAdultos = 1;

    @NotNull(message = "la cantidad de niños es obligatoria")
    @Min(value = 0, message = "la cantidad de niños no puede ser negativa")
    private Integer cantidadNinos = 0;

    @NotNull(message = "la cantidad de bebés es obligatoria")
    @Min(value = 0, message = "la cantidad de bebés no puede ser negativa")
    private Integer cantidadBebes = 0;

    @AssertTrue(message = "debe haber al menos un pasajero")
    public boolean isAlMenosUnPasajero() {
        if (cantidadAdultos == null || cantidadNinos == null || cantidadBebes == null) {
            return true;
        }
        return (long) cantidadAdultos + cantidadNinos + cantidadBebes > 0;
    }

    @AssertTrue(message = "cada bebé debe viajar acompañado por un adulto distinto")
    public boolean isBebesConAdulto() {
        return cantidadAdultos == null || cantidadBebes == null || cantidadBebes <= cantidadAdultos;
    }
}
