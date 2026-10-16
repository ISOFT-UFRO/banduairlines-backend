package com.banduairlines.dto.tracking;

import java.time.LocalDateTime;

import com.banduairlines.model.enums.EstadoVuelo;
import com.banduairlines.model.enums.OrigenEvento;

// Sin actualizadoPor: las consultas son de pasajeros y expondría ids internos de Cuentas.
public record EventoEstadoDTO(
        Long idEvento,
        EstadoVuelo estadoAnterior,
        EstadoVuelo estadoNuevo,
        LocalDateTime fechaCambio,
        OrigenEvento origen,
        String motivo
) {
}
