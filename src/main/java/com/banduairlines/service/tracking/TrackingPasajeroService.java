package com.banduairlines.service.tracking;

import java.util.List;

import com.banduairlines.dto.tracking.VueloPasajeroDTO;

// RF-04: consulta del estado de vuelos por parte del pasajero.
public interface TrackingPasajeroService {

    // Pasajero autenticado: estado de los vuelos de sus reservas.
    List<VueloPasajeroDTO> consultarMisVuelos();

    /*
     * Visitante (sin token): mecanismo de consulta por código de reserva y número
     * de documento de uno de los pasajeros de esa reserva.
     */
    List<VueloPasajeroDTO> consultarVuelosDeReserva(String codigoReserva, String numeroDocumento);
}
