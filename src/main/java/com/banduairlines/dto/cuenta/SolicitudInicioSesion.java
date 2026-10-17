package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudInicioSesion(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150)
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}
