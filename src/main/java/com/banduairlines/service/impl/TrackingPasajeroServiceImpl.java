package com.banduairlines.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.dto.tracking.ResumenVueloDTO;
import com.banduairlines.repository.SeguimientoVueloRepository;
import com.banduairlines.security.UsuarioActual;
import com.banduairlines.service.tracking.TrackingMapper;
import com.banduairlines.service.tracking.TrackingPasajeroService;
import com.banduairlines.service.tracking.puerto.VuelosDelUsuarioPort;

// El estado vigente sale de SeguimientoVuelo, no de Vuelo.estado (que Tracking no actualiza).
@Service
public class TrackingPasajeroServiceImpl implements TrackingPasajeroService {

    private final VuelosDelUsuarioPort vuelosDelUsuario;
    private final SeguimientoVueloRepository seguimientos;
    private final UsuarioActual usuarioActual;

    public TrackingPasajeroServiceImpl(VuelosDelUsuarioPort vuelosDelUsuario,
                                       SeguimientoVueloRepository seguimientos,
                                       UsuarioActual usuarioActual) {
        this.vuelosDelUsuario = vuelosDelUsuario;
        this.seguimientos = seguimientos;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenVueloDTO> consultarMisVuelos() {
        Long usuarioId = usuarioActual.exigirId();
        return buscarVuelos(vuelosDelUsuario.vueloIdsDeUsuario(usuarioId));
    }

    // Los vuelos sin seguimiento no aparecen: Tracking solo conoce los que sincroniza.
    private List<ResumenVueloDTO> buscarVuelos(List<Long> vueloIds) {
        if (vueloIds.isEmpty()) {
            return List.of();
        }
        return seguimientos.findByVueloIdInOrderBySalidaProgramadaAsc(vueloIds).stream()
                .map(TrackingMapper::aResumenDTO)
                .toList();
    }
}
