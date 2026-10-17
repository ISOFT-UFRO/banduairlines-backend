package com.banduairlines.service.busqueda;

import com.banduairlines.dto.busqueda.FiltroBusquedaRequest;
import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;

import java.util.List;

public interface FiltroBusquedaService {

    List<ItinerarioVueloResponse> filtrarYOrdenar(FiltroBusquedaRequest request);
}
