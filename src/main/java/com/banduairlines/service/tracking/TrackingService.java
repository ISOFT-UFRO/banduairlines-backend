package com.banduairlines.service.tracking;

import com.banduairlines.dto.tracking.SeguimientoVueloDTO;

public interface TrackingService {

    SeguimientoVueloDTO obtenerPorId(Long vueloId);
}
