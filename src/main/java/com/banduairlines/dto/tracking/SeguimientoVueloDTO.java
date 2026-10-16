package com.banduairlines.dto.tracking;

import java.time.LocalDateTime;
import java.util.List;

import com.banduairlines.model.enums.EstadoVuelo;

public record SeguimientoVueloDTO(
        Long idSeguimiento,
        Long vueloId,
        String codigoVuelo,
        String origenIata,
        String destinoIata,
        LocalDateTime salidaProgramada,
        LocalDateTime llegadaProgramada,
        EstadoVuelo estadoActual,
        int retrasoMinutos,
        List<EventoEstadoDTO> historial
) {
}
