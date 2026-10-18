package com.banduairlines.service.tracking.adaptador;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.repository.TrackingVueloLecturaRepository;
import com.banduairlines.service.tracking.puerto.VueloExterno;
import com.banduairlines.service.tracking.puerto.VueloExternoPort;

@Component
@ConditionalOnProperty(name = "tracking.fuente-vuelos", havingValue = "busqueda", matchIfMissing = true)
public class VueloExternoBusquedaAdapter implements VueloExternoPort {

    private final TrackingVueloLecturaRepository vuelos;

    public VueloExternoBusquedaAdapter(TrackingVueloLecturaRepository vuelos) {
        this.vuelos = vuelos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VueloExterno> listarVuelosVigentes(LocalDateTime desde, LocalDateTime hasta) {
        return vuelos.listarEntre(desde, hasta);
    }
}
