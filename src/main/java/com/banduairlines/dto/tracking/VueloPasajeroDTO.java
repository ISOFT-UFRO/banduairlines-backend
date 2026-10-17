package com.banduairlines.dto.tracking;

import java.time.LocalDateTime;

import com.banduairlines.model.enums.EstadoVuelo;

// Sin idSeguimiento: el pasajero consulta el vuelo, no la fila interna del seguimiento.
public record VueloPasajeroDTO(
        Long vueloId,
        String codigoVuelo,
        String origenIata,
        String destinoIata,
        LocalDateTime salidaProgramada,
        LocalDateTime llegadaProgramada,
        EstadoVuelo estadoActual,
        int retrasoMinutos,
        LocalDateTime ultimaActualizacion
) {
}
