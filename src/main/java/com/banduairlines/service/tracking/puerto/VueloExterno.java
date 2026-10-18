package com.banduairlines.service.tracking.puerto;

import java.time.LocalDateTime;

public record VueloExterno(
        Long vueloId,
        String codigoVuelo,
        String origenIata,
        String destinoIata,
        LocalDateTime salidaProgramada,
        LocalDateTime llegadaProgramada
) {
}
