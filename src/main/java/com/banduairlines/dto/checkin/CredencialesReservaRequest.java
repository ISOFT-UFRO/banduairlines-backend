package com.banduairlines.dto.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CredencialesReservaRequest (

    @NotBlank(message = "El codigo de reserva (PNR) es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "El codigo de reserva debe tener 6 caracteres alfanumericos")
    String pnr,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 80, message = "Los apellidos no deben superar los 80 caracteres")
    String apellidos
) {
}
