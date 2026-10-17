package com.banduairlines.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.tracking.VueloPasajeroDTO;
import com.banduairlines.exception.RecursoNoEncontradoException;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.model.enums.EstadoReserva;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.tracking.CodigosTracking;
import com.banduairlines.service.tracking.TrackingMapper;
import com.banduairlines.service.tracking.TrackingPasajeroService;
import com.banduairlines.service.tracking.puerto.ReservaExterna;
import com.banduairlines.service.tracking.puerto.ReservaExternaPort;

// El estado vigente sale de SeguimientoVuelo, no de Vuelo.estado (que Tracking no actualiza).
@Service
public class TrackingPasajeroServiceImpl implements TrackingPasajeroService {

    private final ReservaExternaPort reservas;
    private final SeguimientoVueloRepository seguimientos;
    private final UsuarioActual usuarioActual;

    public TrackingPasajeroServiceImpl(ReservaExternaPort reservas,
                                       SeguimientoVueloRepository seguimientos,
                                       UsuarioActual usuarioActual) {
        this.reservas = reservas;
        this.seguimientos = seguimientos;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VueloPasajeroDTO> consultarMisVuelos() {
        Long usuarioId = usuarioActual.exigirId();
        return buscarVuelos(reservas.vueloIdsDeUsuario(usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VueloPasajeroDTO> consultarVuelosDeReserva(String codigoReserva, String numeroDocumento) {
        String codigo = codigoReserva.trim().toUpperCase();
        ReservaExterna reserva = reservas.buscarPorCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe reserva con el código: " + codigo)
                        .conCodigo(CodigosTracking.RESERVA_NO_ENCONTRADA));

        if (reserva.estado() != EstadoReserva.CONFIRMADA) {
            throw new ReglaNegocioException("Solo es posible consultar los vuelos de una reserva confirmada")
                    .conCodigo(CodigosTracking.RESERVA_NO_CONFIRMADA);
        }

        String documento = numeroDocumento.trim();
        if (!reservas.documentoPerteneceAReserva(reserva.id(), documento)) {
            throw new ReglaNegocioException("El número de documento no corresponde a ningún pasajero de la reserva")
                    .conCodigo(CodigosTracking.DOCUMENTO_NO_VALIDO);
        }

        return buscarVuelos(reservas.vueloIdsDeReserva(reserva.id()));
    }

    // Los vuelos sin seguimiento no aparecen: Tracking solo conoce los que sincroniza.
    private List<VueloPasajeroDTO> buscarVuelos(List<Long> vueloIds) {
        if (vueloIds.isEmpty()) {
            return List.of();
        }
        return seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(vueloIds).stream()
                .map(TrackingMapper::aVueloPasajeroDTO)
                .toList();
    }
}
