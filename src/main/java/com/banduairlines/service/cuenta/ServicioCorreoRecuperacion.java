
package com.banduairlines.service.cuenta;

import java.time.Instant;

public interface ServicioCorreoRecuperacion {

    void enviarEnlaceRecuperacion(
            String destinatario,
            String enlaceRecuperacion,
            Instant fechaExpiracion
    );
}
