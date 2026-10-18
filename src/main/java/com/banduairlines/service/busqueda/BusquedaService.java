package com.banduairlines.service.busqueda;

import java.util.List;
import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.AlternativaVueloResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.OrdenVuelo;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;

public interface BusquedaService {
    List<VueloBusquedaResponse> buscarVuelos(BusquedaVueloRequest criterios);
    List<AlternativaVueloResponse> compararVuelos(BusquedaVueloRequest criterios, OrdenVuelo orden);
    List<AeropuertoBusquedaResponse> listarAeropuertos();
}
