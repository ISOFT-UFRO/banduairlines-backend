package com.banduairlines.dto.busqueda;

import java.time.LocalDateTime;
import com.banduairlines.model.enums.EstadoVuelo;

public record VueloBusquedaResponse(Long id, String codigoVuelo,
        AeropuertoBusquedaResponse origen, AeropuertoBusquedaResponse destino,
        LocalDateTime salidaProgramada, LocalDateTime llegadaProgramada, EstadoVuelo estado) {
}
