package com.banduairlines.dto.checkin;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CancelacionInvitadoRequest(
        @NotBlank(message = "el código de reserva (PNR) es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "el código de reserva debe tener 6 caracteres alfanuméricos")
        String pnr,

        @NotBlank(message = "los apellidos son obligatorios")
        @Size(max = 80, message = "los apellidos no pueden superar los 80 caracteres")
        String apellidos,

        @JsonProperty("motivo_cancelacion")
        @Size(max = 255, message = "el motivo no puede superar los 255 caracteres")
        String motivoCancelacion
) {
}
