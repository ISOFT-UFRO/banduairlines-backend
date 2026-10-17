package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.NotBlank;

public record SolicitudEliminacionCuenta(
        @NotBlank(message = "La contrasena actual es obligatoria") String contrasenaActual) { }
