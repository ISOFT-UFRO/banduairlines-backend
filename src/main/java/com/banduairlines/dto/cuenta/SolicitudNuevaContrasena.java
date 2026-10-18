package com.banduairlines.dto.cuenta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudNuevaContrasena(

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(
                min = 8,
                max = 32,
                message = "La contrasena debe tener entre 8 y 32 caracteres"
        )
        @Pattern(
                regexp = "[\\p{L}\\p{N}.-]+",
                message = "Solo se permiten letras, numeros, puntos y guiones"
        )
        String nuevaContrasena,

        @NotBlank(message = "La confirmacion es obligatoria")
        @Size(
                min = 8,
                max = 32
        )
        @Pattern(
                regexp = "[\\p{L}\\p{N}.-]+",
                message = "Solo se permiten letras, numeros, puntos y guiones"
        )
        String confirmacionContrasena

) {
}
