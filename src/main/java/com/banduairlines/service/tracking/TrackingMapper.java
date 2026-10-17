package com.banduairlines.service.tracking;

import java.util.List;

import com.banduairlines.dto.tracking.EventoEstadoDTO;
import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.dto.tracking.SeguimientoVueloDTO;
import com.banduairlines.dto.tracking.VueloPasajeroDTO;
import com.banduairlines.model.EventoEstado;
import com.banduairlines.model.SeguimientoVuelo;

// Usar dentro de la transacción: el historial es lazy y open-in-view está desactivado.
public final class TrackingMapper {

    private TrackingMapper() {
    }

    public static SeguimientoVueloDTO aSeguimientoDTO(SeguimientoVuelo s) {
        List<EventoEstadoDTO> historial = s.historial().stream()
                .map(TrackingMapper::aEventoDTO)
                .toList();
        return new SeguimientoVueloDTO(
                s.getId(),
                s.getVueloId(),
                s.getCodigoVuelo(),
                s.getOrigenIata(),
                s.getDestinoIata(),
                s.getSalidaProgramada(),
                s.getLlegadaProgramada(),
                s.getEstadoActual(),
                s.retrasoVigente(),
                historial
        );
    }

    public static EventoEstadoDTO aEventoDTO(EventoEstado e) {
        return new EventoEstadoDTO(
                e.getId(),
                e.getEstadoAnterior(),
                e.getEstadoNuevo(),
                e.getFechaCambio(),
                e.getOrigen(),
                e.getMotivo()
        );
    }

    public static ResumenVueloDTO aResumenDTO(SeguimientoVuelo s) {
        return new ResumenVueloDTO(
                s.getId(),
                s.getVueloId(),
                s.getCodigoVuelo(),
                s.getOrigenIata(),
                s.getDestinoIata(),
                s.getSalidaProgramada(),
                s.getEstadoActual(),
                s.retrasoVigente(),
                s.getUltimaActualizacion()
        );
    }

    public static VueloPasajeroDTO aVueloPasajeroDTO(SeguimientoVuelo s) {
        return new VueloPasajeroDTO(
                s.getVueloId(),
                s.getCodigoVuelo(),
                s.getOrigenIata(),
                s.getDestinoIata(),
                s.getSalidaProgramada(),
                s.getLlegadaProgramada(),
                s.getEstadoActual(),
                s.retrasoVigente(),
                s.getUltimaActualizacion()
        );
    }
}
