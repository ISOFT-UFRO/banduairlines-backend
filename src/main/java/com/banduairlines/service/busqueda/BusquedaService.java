package com.banduairlines.service.busqueda;

import java.util.List;
import com.banduairlines.dto.busqueda.AeropuertoBusquedaResponse;
import com.banduairlines.dto.busqueda.AlternativaVueloResponse;
import com.banduairlines.dto.busqueda.BusquedaVueloRequest;
import com.banduairlines.dto.busqueda.OrdenVuelo;
import com.banduairlines.dto.busqueda.PasajerosBusquedaRequest;
import com.banduairlines.dto.busqueda.VueloBusquedaResponse;

public interface BusquedaService {
    default List<VueloBusquedaResponse> buscarVuelos(BusquedaVueloRequest criterios) {
        return buscarVuelos(criterios, new PasajerosBusquedaRequest());
    }

    List<VueloBusquedaResponse> buscarVuelos(BusquedaVueloRequest criterios, PasajerosBusquedaRequest pasajeros);

    default List<AlternativaVueloResponse> compararVuelos(BusquedaVueloRequest criterios, OrdenVuelo orden) {
        return compararVuelos(criterios, orden, new PasajerosBusquedaRequest());
    }

    List<AlternativaVueloResponse> compararVuelos(BusquedaVueloRequest criterios, OrdenVuelo orden,
                                               PasajerosBusquedaRequest pasajeros);
    List<AeropuertoBusquedaResponse> listarAeropuertos();
}
