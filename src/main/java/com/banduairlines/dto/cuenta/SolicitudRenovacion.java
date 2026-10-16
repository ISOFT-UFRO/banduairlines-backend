package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.NotBlank;

public record SolicitudRenovacion(

        @NotBlank(
                message = "La credencial de renovacion es obligatoria"
        )
        String credencialRenovacion

) {
}
