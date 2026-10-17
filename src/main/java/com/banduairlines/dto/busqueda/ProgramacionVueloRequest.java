package com.banduairlines.dto.busqueda;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProgramacionVueloRequest(
        @NotBlank(message = "El código de vuelo es obligatorio")
        @Size(max = 10, message = "El código de vuelo no puede superar los 10 caracteres")
        String codigoVuelo,

        @NotNull(message = "El ID de la ruta es obligatorio")
        @Positive(message = "El ID de la ruta debe ser un número positivo")
        Long rutaId,

        @NotNull(message = "El ID de la aeronave es obligatorio")
        @Positive(message = "El ID de la aeronave debe ser un número positivo")
        Long aeronaveId,

        @NotNull(message = "La fecha y hora de salida programada es obligatoria")
        @Future(message = "La fecha y hora de salida programada debe ser futura")
        LocalDateTime salidaProgramada,

        @NotNull(message = "La fecha y hora de llegada programada es obligatoria")
        @Future(message = "La fecha y hora de llegada programada debe ser futura")
        LocalDateTime llegadaProgramada
) {}