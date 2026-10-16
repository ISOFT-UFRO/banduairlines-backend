package com.banduairlines.service.busqueda;

import com.banduairlines.dto.busqueda.PasajerosBusquedaRequest;
import com.banduairlines.dto.busqueda.ResumenPasajerosResponse;

public interface PasajerosBusquedaService {

    ResumenPasajerosResponse validarYResumir(PasajerosBusquedaRequest request);
}
