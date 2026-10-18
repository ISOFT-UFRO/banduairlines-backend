package com.banduairlines.service.admin;

import com.banduairlines.dto.admin.AvionRequest;
import com.banduairlines.dto.admin.AvionResponse;

public interface AvionService {

    AvionResponse crearAvion(AvionRequest solicitud);

    AvionResponse obtenerPorId(Long id);
}