package com.banduairlines.dto.tracking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.banduairlines.model.enums.EstadoVuelo;

// La obligatoriedad del motivo la valida el dominio (422 MOTIVO_REQUERIDO).
public record ActualizarEstadoRequest(
        @NotNull(message = "el nuevo estado es obligatorio")
        EstadoVuelo nuevoEstado,

        @Size(max = 500, message = "el motivo no puede superar los 500 caracteres")
        String motivo,

        @Positive(message = "los minutos de retraso deben ser mayores a cero")
        Integer retrasoMinutos
) {
}
