package com.banduairlines.service;

import java.util.List;
import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;

public interface BusquedaService {
    List<VueloBusquedaResponse> buscarVuelos(BusquedaVueloRequest criterios);
    List<AeropuertoBusquedaResponse> listarAeropuertos();
}
