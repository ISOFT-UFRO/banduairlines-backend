package com.banduairlines.service.cuenta;

import java.time.Instant;
import java.util.Optional;

public interface SolicitudesRecuperacionService {

    TokenEmitido crearSolicitud(Long idUsuario);

    boolean esValido(String token);

    Optional<Long> consumirSolicitud(String token);

    EstadoToken estadoToken(String token);

    enum EstadoToken {
        VALIDO,
        INVALIDO,
        VENCIDO,
        UTILIZADO
    }

    record TokenEmitido(
            String token,
            Instant fechaExpiracion
    ) {
    }
}
