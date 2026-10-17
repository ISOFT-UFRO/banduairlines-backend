package com.banduairlines.service.busqueda;

import java.util.List;

import com.banduairlines.dto.busqueda.ItinerarioVueloResponse;
import com.banduairlines.dto.busqueda.ResumenPasajerosResponse;
import com.banduairlines.dto.busqueda.TarifasBusquedaRequest;
import com.banduairlines.dto.busqueda.TarifasBusquedaResponse;

public interface TarifasBusquedaService {

    TarifasBusquedaResponse consultar(TarifasBusquedaRequest request);

    List<ItinerarioVueloResponse> incorporarTarifas(
            List<ItinerarioVueloResponse> itinerarios, ResumenPasajerosResponse pasajeros);
}
