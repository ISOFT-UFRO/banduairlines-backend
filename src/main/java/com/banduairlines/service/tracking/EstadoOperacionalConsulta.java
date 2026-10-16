package com.banduairlines.service.tracking;

import java.util.Collection;
import java.util.Map;

// Para Check-In: estado y retraso de los vuelos de sus itinerarios (no usar Vuelo.estado, que Tracking no actualiza).
public interface EstadoOperacionalConsulta {

    // Los vuelos sin seguimiento no aparecen en el mapa.
    Map<Long, EstadoOperacional> estadosDeVuelos(Collection<Long> vueloIds);
}
