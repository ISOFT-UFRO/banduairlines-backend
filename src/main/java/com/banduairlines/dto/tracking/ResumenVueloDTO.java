package com.banduairlines.dto.tracking;

import java.time.LocalDateTime;

import com.banduairlines.model.enums.EstadoVuelo;

public record ResumenVueloDTO(
        Long idSeguimiento,
        Long vueloId,
        String codigoVuelo,
        String origenIata,
        String destinoIata,
        LocalDateTime salidaProgramada,
        EstadoVuelo estadoActual,
        int retrasoMinutos,
        LocalDateTime ultimaActualizacion
) {
}
