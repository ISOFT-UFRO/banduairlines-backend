package com.banduairlines.service.busqueda;

import com.banduairlines.dto.busqueda.BusquedaItinerarioRequest;
import com.banduairlines.dto.busqueda.BusquedaItinerarioResponse;

public interface ItinerarioBusquedaService {

    BusquedaItinerarioResponse buscar(BusquedaItinerarioRequest request);
}
