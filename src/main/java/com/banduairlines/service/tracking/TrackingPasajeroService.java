package com.banduairlines.service.tracking;

import java.util.List;

import com.banduairlines.dto.tracking.ResumenVueloDTO;

// RF-04: consulta del estado de vuelos por parte del pasajero.
public interface TrackingPasajeroService {

    // Pasajero autenticado: estado de los vuelos de sus reservas.
    List<ResumenVueloDTO> consultarMisVuelos();
}
