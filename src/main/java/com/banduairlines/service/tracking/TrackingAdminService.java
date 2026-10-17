package com.banduairlines.service.tracking;

import com.banduairlines.dto.tracking.ActualizarEstadoRequest;
import com.banduairlines.dto.tracking.SeguimientoVueloDTO;

public interface TrackingAdminService {

    SeguimientoVueloDTO actualizarEstadoVuelo(Long vueloId, ActualizarEstadoRequest request);
}