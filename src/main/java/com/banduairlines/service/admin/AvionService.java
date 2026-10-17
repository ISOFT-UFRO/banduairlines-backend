package com.banduairlines.service.admin;

import com.banduairlines.dto.admin.AvionRequestDto;
import com.banduairlines.dto.admin.AvionResponseDto;

public interface AvionService {

    AvionResponseDto crearAvion(AvionRequestDto solicitud);

    AvionResponseDto obtenerPorId(Long id);
}