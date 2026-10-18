package com.banduairlines.dto.cuenta;

import com.banduairlines.model.enums.Nivel;
import com.banduairlines.model.enums.RolUsuario;

public record RespuestaSesion(
        String credencialAcceso,
        String credencialRenovacion,
        String tipoCredencial,
        long segundosExpiracion,
        DatosUsuario usuario
) {

    public record DatosUsuario(
            Long idUsuario,
            String nombres,
            String apellidos,
            String correo,
            RolUsuario rol,
            Nivel nivel
    ) {
    }
}
