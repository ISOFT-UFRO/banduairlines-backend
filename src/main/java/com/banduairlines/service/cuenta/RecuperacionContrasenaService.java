package com.banduairlines.service.cuenta;

import com.banduairlines.dto.cuenta.SolicitudNuevaContrasena;

public interface RecuperacionContrasenaService {

    void solicitarRecuperacion(String correo);

    void validarRecuperacion(String codigoRecuperacion);

    void restablecerContrasena(
            String codigoRecuperacion,
            SolicitudNuevaContrasena solicitud
    );
}
